package com.cards.game.literature

class WasmPlatform : Platform {
    override val name: String = "Web"
}

actual fun getPlatform(): Platform = WasmPlatform()

// No-op: on wasm, kotlin.Error is how JS-interop failures arrive (Ktor wraps fetch
// rejections in Error) — nothing here is process-fatal the way a JVM OOM is.
actual fun rethrowIfPlatformFatal(e: Throwable) {}
