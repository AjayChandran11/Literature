package com.cards.game.literature.preferences

import kotlinx.browser.localStorage
import kotlinx.browser.window
import org.w3c.dom.Storage

/**
 * Every browser-storage touch, guarded.
 *
 * A browser with site data blocked throws on the `localStorage` property access itself, not just
 * on get and set — Safari with cross-site tracking prevention in an embedded view, Chrome with
 * third-party cookies blocked in some framings, older WebKit private mode. The app's first read
 * happens while navigation composes, so that throw took the whole page down: splash, then blank,
 * no error anywhere the player could see.
 *
 * Reads fall back to an in-memory map. Settings then hold for the session and are simply not
 * remembered next visit, which is the right outcome for someone who blocked storage on purpose.
 *
 * runCatching, not try/catch on Exception: a JS exception surfaces as a [Throwable] that is not
 * an Exception, so catching Exception here would miss the very failure this exists for.
 */
internal object WebStorage {

    private val memory = mutableMapOf<String, String>()

    private fun local(): Storage? = runCatching { localStorage }.getOrNull()

    private fun session(): Storage? = runCatching { window.sessionStorage }.getOrNull()

    fun get(key: String): String? =
        runCatching { local()?.getItem(key) }.getOrNull() ?: memory[key]

    fun set(key: String, value: String) {
        memory[key] = value
        runCatching { local()?.setItem(key, value) }
    }

    fun remove(key: String) {
        memory.remove(key)
        runCatching { local()?.removeItem(key) }
    }

    /** Session-scoped twin, for state that should die with the tab. */
    fun getSession(key: String): String? =
        runCatching { session()?.getItem(key) }.getOrNull() ?: memory["s:$key"]

    fun setSession(key: String, value: String) {
        memory["s:$key"] = value
        runCatching { session()?.setItem(key, value) }
    }

    fun removeSession(key: String) {
        memory.remove("s:$key")
        runCatching { session()?.removeItem(key) }
    }
}
