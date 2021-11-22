package deep

import serial.*
import java.io.Reader

public fun interface ValueParser<out T> {
    public fun TextParseState.parseValue(): T
}

public fun <T> Reader.parseDeep(parser: ValueParser<T>): Deep<T> = parse {
    skipWhitespace()
    return parseDeep(parser)
}

private fun <T> TextParseState.parseDeep(parser: ValueParser<T>): Deep<T> = when (current) {
    '{' -> parseMap(parser)
    '[' -> parseList(parser)
    else -> DeepValue(with(parser) { parseValue() })
}

private fun <T> TextParseState.parseMap(parser: ValueParser<T>): DeepMap<T> {
    val map = mutableMapOf<String, Deep<T>>()
    parseCollection('}') {
        val key = decodeStringLiteral()
        skipWhitespace()
        if (!readOptionalChar(':')) readRequiredChar('=')
        skipWhitespace()
        map[key] = parseDeep(parser)
    }
    return DeepMap(map)
}

private fun <T> TextParseState.parseList(parser: ValueParser<T>): DeepList<T> {
    val list = mutableListOf<Deep<T>>()
    parseCollection(']') {
        list += parseDeep(parser)
    }
    return DeepList(list)
}

private inline fun TextParseState.parseCollection(end: Char, action: () -> Unit) {
    next()
    do {
        skipWhitespace()
        if (readOptionalChar(end)) return
        action()
        skipWhitespace()
    } while (readOptionalChar(','))
    readRequiredChar(end)
}

public fun TextParseState.decodeStringLiteral(): String {
    val delimiter = current
    ensure(delimiter == '"' || delimiter == '\'') { "Expected: \" or '" }
    next()
    startCapture()
    while (current != delimiter) {
        ensure(current >= '\u0020') { "Invalid character" }
        if (current == '\\') {
            pauseCapture()
            next()
            addToCapture(
                when (current) {
                    'n' -> '\n'
                    'r' -> '\r'
                    't' -> '\t'
                    'b' -> '\b'
                    'f' -> '\u000c'
                    'u' -> String(CharArray(4) {
                        next()
                        ensure(current.isHexDigit()) { "Invalid hex digit" }
                        current
                    }).toInt(16).toChar()
                    else -> current
                }
            )
            next()
            startCapture()
        } else {
            next()
        }
    }
    val string = finishCapture()
    next()
    return string
}

public fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
