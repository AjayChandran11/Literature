package com.cards.game.literature.server

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Duration
import java.util.Base64

/** The one HTTP shape this needs: a POST with a body, optionally bearer-authorised. */
fun interface HttpTransport {
    fun post(url: String, contentType: String, body: String, bearer: String?): HttpResult
}

data class HttpResult(val status: Int, val body: String)

/** java.net.http, so the server image gains no dependency for this. */
object JdkHttpTransport : HttpTransport {
    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()

    override fun post(url: String, contentType: String, body: String, bearer: String?): HttpResult {
        val request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", contentType)
            .apply { if (bearer != null) header("Authorization", "Bearer $bearer") }
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return HttpResult(response.statusCode(), response.body())
    }
}

/** The fields of a Firebase service-account key file that the sender uses. */
class ServiceAccount(
    val projectId: String,
    val clientEmail: String,
    val tokenUri: String,
    val privateKey: PrivateKey,
) {
    companion object {
        /** Parses the JSON Firebase hands out under Project settings → Service accounts. */
        fun parse(json: String): ServiceAccount {
            val obj = Json.parseToJsonElement(json).jsonObject
            fun field(name: String) = obj[name]?.jsonPrimitive?.content ?: error("service account JSON has no \"$name\"")
            return ServiceAccount(
                projectId = field("project_id"),
                clientEmail = field("client_email"),
                tokenUri = obj["token_uri"]?.jsonPrimitive?.content ?: "https://oauth2.googleapis.com/token",
                privateKey = parsePrivateKey(field("private_key")),
            )
        }

        private fun parsePrivateKey(pem: String): PrivateKey {
            val base64 = pem.lineSequence()
                .filterNot { it.startsWith("-----") }
                .joinToString("") { it.trim() }
            val bytes = Base64.getDecoder().decode(base64)
            return KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(bytes))
        }
    }
}

/**
 * Pushes through Firebase Cloud Messaging's HTTP v1 API, authorised with a short-lived OAuth
 * token minted from the service account: a JWT signed with its key and exchanged at Google's
 * token endpoint. Written out here rather than pulled in as Google's auth library, which brings
 * several megabytes of transitive dependencies to a 512 MB instance for what is eighty lines.
 *
 * Every send runs on its own coroutine; the rooms never wait on Google.
 */
class FcmPushSender(
    private val account: ServiceAccount,
    private val transport: HttpTransport = JdkHttpTransport,
    private val now: () -> Long = System::currentTimeMillis,
) : PushSender {
    private val log = LoggerFactory.getLogger("Push")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tokenLock = Mutex()
    private var accessToken: String? = null
    private var accessTokenExpiresAt = 0L

    override fun send(token: String, type: PushType, data: Map<String, String>, onInvalidToken: () -> Unit) {
        scope.launch {
            try {
                deliver(token, type, data, onInvalidToken)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.warn("Push {} failed: {}", type.wire, e.toString())
            }
        }
    }

    /** The send itself, on the caller's coroutine; [send] wraps it so tests can drive it directly. */
    internal suspend fun deliver(token: String, type: PushType, data: Map<String, String>, onInvalidToken: () -> Unit) {
        val body = messageBody(token, type, data)
        var result = transport.post(sendUrl, JSON_UTF8, body, accessToken())
        if (result.status == 401) {
            // The access token was revoked under us: mint another and try once more.
            invalidateAccessToken()
            result = transport.post(sendUrl, JSON_UTF8, body, accessToken())
        }
        when {
            result.status in 200..299 -> log.debug("Push {} delivered", type.wire)
            isDeadToken(result) -> {
                log.info("Push token no longer valid; forgetting it")
                onInvalidToken()
            }
            else -> log.warn("Push {} rejected: HTTP {} {}", type.wire, result.status, result.body.take(300))
        }
    }

    private val sendUrl: String
        get() = "https://fcm.googleapis.com/v1/projects/${account.projectId}/messages:send"

    /**
     * FCM answers UNREGISTERED (404) for a token whose app was uninstalled or re-registered, and
     * INVALID_ARGUMENT (400) for one that never was a token. Neither is worth keeping.
     */
    private fun isDeadToken(result: HttpResult): Boolean =
        (result.status == 404 && result.body.contains("UNREGISTERED")) ||
            (result.status == 400 && result.body.contains("INVALID_ARGUMENT") &&
                result.body.contains("token", ignoreCase = true))

    internal fun messageBody(token: String, type: PushType, data: Map<String, String>): String = buildJsonObject {
        putJsonObject("message") {
            put("token", token)
            putJsonObject("data") { data.forEach { (key, value) -> put(key, value) } }
            putJsonObject("android") {
                put("priority", if (type.highPriority) "HIGH" else "NORMAL")
                put("ttl", "${type.ttlSeconds}s")
            }
        }
    }.toString()

    private suspend fun accessToken(): String = tokenLock.withLock {
        val cached = accessToken
        if (cached != null && now() < accessTokenExpiresAt - 60_000) return cached
        val assertion = signedJwt(now() / 1000)
        val form = "grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer", "UTF-8") +
            "&assertion=" + URLEncoder.encode(assertion, "UTF-8")
        val result = transport.post(account.tokenUri, "application/x-www-form-urlencoded", form, bearer = null)
        if (result.status !in 200..299) error("token endpoint answered HTTP ${result.status}: ${result.body.take(300)}")
        val obj = Json.parseToJsonElement(result.body).jsonObject
        val fresh = obj["access_token"]?.jsonPrimitive?.content ?: error("token endpoint sent no access_token")
        val expiresIn = obj["expires_in"]?.jsonPrimitive?.longOrNull ?: 3600L
        accessToken = fresh
        accessTokenExpiresAt = now() + expiresIn * 1000
        fresh
    }

    private suspend fun invalidateAccessToken() = tokenLock.withLock { accessToken = null }

    /** An RS256 JWT asking for the messaging scope, valid for an hour: Google's service-account flow. */
    internal fun signedJwt(nowSeconds: Long): String {
        val header = """{"alg":"RS256","typ":"JWT"}"""
        val claims = buildJsonObject {
            put("iss", account.clientEmail)
            put("scope", "https://www.googleapis.com/auth/firebase.messaging")
            put("aud", account.tokenUri)
            put("iat", nowSeconds)
            put("exp", nowSeconds + 3600)
        }.toString()
        val unsigned = base64Url(header.toByteArray()) + "." + base64Url(claims.toByteArray())
        val signature = Signature.getInstance("SHA256withRSA").apply {
            initSign(account.privateKey)
            update(unsigned.toByteArray())
        }.sign()
        return unsigned + "." + base64Url(signature)
    }

    private fun base64Url(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    companion object {
        const val ENV_SERVICE_ACCOUNT = "FCM_SERVICE_ACCOUNT_JSON"
        private const val JSON_UTF8 = "application/json; charset=utf-8"

        /** The sender the environment asks for: FCM when the key is set, nothing otherwise. */
        fun fromEnvironment(json: String? = System.getenv(ENV_SERVICE_ACCOUNT)): PushSender {
            val log = LoggerFactory.getLogger("Push")
            if (json.isNullOrBlank()) {
                log.info("Push notifications off: {} is not set", ENV_SERVICE_ACCOUNT)
                return PushSender.None
            }
            return try {
                val account = ServiceAccount.parse(json)
                log.info("Push notifications on for Firebase project {}", account.projectId)
                FcmPushSender(account)
            } catch (e: Exception) {
                log.error("Push notifications off: {} could not be read ({})", ENV_SERVICE_ACCOUNT, e.toString())
                PushSender.None
            }
        }
    }
}
