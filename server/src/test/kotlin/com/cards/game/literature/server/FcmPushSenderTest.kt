package com.cards.game.literature.server

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The FCM client against a scripted HTTP layer: Google's token exchange, the message shape, and
 * the two answers that matter (a dead token, a revoked access token). The signature is checked
 * with the matching public key, so the JWT really is what Google will verify.
 */
class FcmPushSenderTest {

    private val keys: KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    private fun serviceAccountJson(): String {
        val body = Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(keys.private.encoded)
        val pem = "-----BEGIN PRIVATE KEY-----\n$body\n-----END PRIVATE KEY-----\n"
        return buildJsonObject {
            put("type", "service_account")
            put("project_id", "literature-test")
            put("client_email", "push@literature-test.iam.gserviceaccount.com")
            put("private_key", pem)
            put("token_uri", "https://oauth2.example.test/token")
        }.toString()
    }

    private class ScriptedTransport : HttpTransport {
        data class Call(val url: String, val contentType: String, val body: String, val bearer: String?)
        val calls = mutableListOf<Call>()
        val sendAnswers = ArrayDeque<HttpResult>()
        var tokensMinted = 0
        override fun post(url: String, contentType: String, body: String, bearer: String?): HttpResult {
            calls += Call(url, contentType, body, bearer)
            return if (url.endsWith("/token")) {
                tokensMinted++
                HttpResult(200, """{"access_token":"tok-$tokensMinted","expires_in":3600,"token_type":"Bearer"}""")
            } else {
                sendAnswers.removeFirstOrNull() ?: HttpResult(200, """{"name":"projects/literature-test/messages/1"}""")
            }
        }
        fun sends() = calls.filterNot { it.url.endsWith("/token") }
    }

    private fun sender(transport: ScriptedTransport, now: () -> Long = { 1_700_000_000_000L }) =
        FcmPushSender(ServiceAccount.parse(serviceAccountJson()), transport, now)

    private fun b64(s: String) = String(Base64.getUrlDecoder().decode(s))

    @Test
    fun theJwtIsSignedWithTheAccountKeyAndAsksForMessaging() {
        val jwt = sender(ScriptedTransport()).signedJwt(nowSeconds = 1_700_000_000L)
        val (header, claims, signature) = jwt.split('.').also { assertEquals(3, it.size) }

        assertEquals("RS256", Json.parseToJsonElement(b64(header)).jsonObject["alg"]!!.jsonPrimitive.content)
        val c = Json.parseToJsonElement(b64(claims)).jsonObject
        assertEquals("push@literature-test.iam.gserviceaccount.com", c["iss"]!!.jsonPrimitive.content)
        assertEquals("https://www.googleapis.com/auth/firebase.messaging", c["scope"]!!.jsonPrimitive.content)
        assertEquals("https://oauth2.example.test/token", c["aud"]!!.jsonPrimitive.content)
        assertEquals(3600L, c["exp"]!!.jsonPrimitive.content.toLong() - c["iat"]!!.jsonPrimitive.content.toLong())

        val verifier = Signature.getInstance("SHA256withRSA").apply {
            initVerify(keys.public)
            update("$header.$claims".toByteArray())
        }
        assertTrue(verifier.verify(Base64.getUrlDecoder().decode(signature)), "Google will check exactly this")
    }

    @Test
    fun oneAccessTokenServesManySendsAndTheMessageCarriesTheData() = runBlocking {
        val transport = ScriptedTransport()
        val fcm = sender(transport)

        fcm.deliver("device-1", PushType.YOUR_TURN, mapOf("type" to "your_turn", "roomCode" to "ABCDEF")) {}
        fcm.deliver("device-2", PushType.GAME_OVER, mapOf("type" to "game_over", "result" to "won")) {}

        assertEquals(1, transport.tokensMinted, "the access token lives an hour; it is minted once")
        val sends = transport.sends()
        assertEquals(2, sends.size)
        assertTrue(sends.all { it.bearer == "tok-1" && it.url == "https://fcm.googleapis.com/v1/projects/literature-test/messages:send" })

        val first = Json.parseToJsonElement(sends[0].body).jsonObject["message"]!!.jsonObject
        assertEquals("device-1", first["token"]!!.jsonPrimitive.content)
        assertEquals("ABCDEF", first["data"]!!.jsonObject["roomCode"]!!.jsonPrimitive.content)
        assertEquals("HIGH", first["android"]!!.jsonObject["priority"]!!.jsonPrimitive.content)
        assertEquals("60s", first["android"]!!.jsonObject["ttl"]!!.jsonPrimitive.content, "a turn push dies with the turn clock")
        val second = Json.parseToJsonElement(sends[1].body).jsonObject["message"]!!.jsonObject
        assertEquals("NORMAL", second["android"]!!.jsonObject["priority"]!!.jsonPrimitive.content)
    }

    @Test
    fun theAccessTokenIsMintedAgainOnceItIsAboutToExpire() = runBlocking {
        var now = 1_700_000_000_000L
        val transport = ScriptedTransport()
        val fcm = sender(transport) { now }

        fcm.deliver("d", PushType.YOUR_TURN, emptyMap()) {}
        now += 3600_000L - 30_000L // half a minute before expiry: too close to trust
        fcm.deliver("d", PushType.YOUR_TURN, emptyMap()) {}

        assertEquals(2, transport.tokensMinted)
        assertEquals("tok-2", transport.sends().last().bearer)
    }

    @Test
    fun aDeadTokenIsReportedAndAnOutageIsNot() = runBlocking {
        val transport = ScriptedTransport()
        val fcm = sender(transport)
        var dead = 0

        transport.sendAnswers += HttpResult(404, """{"error":{"code":404,"status":"NOT_FOUND","details":[{"errorCode":"UNREGISTERED"}]}}""")
        fcm.deliver("gone", PushType.GAME_STARTED, emptyMap()) { dead++ }
        assertEquals(1, dead)

        transport.sendAnswers += HttpResult(500, """{"error":{"status":"INTERNAL"}}""")
        fcm.deliver("fine", PushType.GAME_STARTED, emptyMap()) { dead++ }
        assertEquals(1, dead, "a server-side failure says nothing about the token")
    }

    @Test
    fun aRevokedAccessTokenIsReplacedAndTheSendRetriedOnce() = runBlocking {
        val transport = ScriptedTransport()
        val fcm = sender(transport)

        transport.sendAnswers += HttpResult(401, """{"error":{"status":"UNAUTHENTICATED"}}""")
        fcm.deliver("d", PushType.REMATCH, emptyMap()) {}

        assertEquals(2, transport.tokensMinted)
        assertEquals(listOf("tok-1", "tok-2"), transport.sends().map { it.bearer })
    }

    @Test
    fun withoutAUsableKeyThereIsNoSender() {
        assertIs<PushSender.None>(FcmPushSender.fromEnvironment(null))
        assertIs<PushSender.None>(FcmPushSender.fromEnvironment("   "))
        assertIs<PushSender.None>(FcmPushSender.fromEnvironment("""{"project_id":"x"}"""), "a key file missing its fields turns push off rather than crashing the server")
        assertIs<FcmPushSender>(FcmPushSender.fromEnvironment(serviceAccountJson()))
    }
}
