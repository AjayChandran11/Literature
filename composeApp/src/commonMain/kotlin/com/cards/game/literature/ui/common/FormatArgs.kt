package com.cards.game.literature.ui.common

/**
 * Multiplatform stand-in for JVM String.format over positional %N$s / %N$d placeholders —
 * the only shapes our resource strings use. (String.format doesn't exist on wasm/JS.)
 */
fun String.formatArgs(vararg args: Any?): String {
    // Single pass over the template: substituted values are never rescanned, so an
    // argument that itself contains "%2$s" (player names are user input) stays literal.
    val out = StringBuilder(length + 16)
    var i = 0
    while (i < length) {
        val c = this[i]
        if (c == '%' && i + 3 < length && this[i + 1] in '1'..'9' && this[i + 2] == '$' &&
            (this[i + 3] == 's' || this[i + 3] == 'd')
        ) {
            val argIndex = this[i + 1] - '1'
            if (argIndex < args.size) out.append(args[argIndex].toString())
            else out.append(this, i, i + 4)
            i += 4
        } else {
            out.append(c)
            i++
        }
    }
    return out.toString()
}
