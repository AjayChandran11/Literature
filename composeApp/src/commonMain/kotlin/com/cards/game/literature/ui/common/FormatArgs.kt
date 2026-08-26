package com.cards.game.literature.ui.common

/**
 * Multiplatform stand-in for JVM String.format over positional %N$s / %N$d placeholders —
 * the only shapes our resource strings use. (String.format doesn't exist on wasm/JS.)
 */
fun String.formatArgs(vararg args: Any?): String {
    var result = this
    args.forEachIndexed { index, arg ->
        val value = arg.toString()
        result = result
            .replace("%${index + 1}\$s", value)
            .replace("%${index + 1}\$d", value)
    }
    return result
}
