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

private fun <T> TextParseState.parseDeep(parser: ValueParser<T>): Deep<T> = when (char) {
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

// TODO: Allow trailing commas
private inline fun TextParseState.parseCollection(end: Char, action: () -> Unit) {
    next()
    skipWhitespace()
    if (readOptionalChar(end)) return
    do {
        skipWhitespace()
        action()
        skipWhitespace()
    } while (readOptionalChar(','))
    readRequiredChar(end)
}

public fun TextParseState.decodeStringLiteral(): String {
    val delimiter = char
    ensure(delimiter == '"' || delimiter == '\'') { "Expected: \" or '" }
    next()
    startCapture()
    while (char != delimiter) {
        ensure(char >= '\u0020') { "Invalid character" }
        if (char == '\\') {
            pauseCapture()
            next()
            addToCapture(
                when (char) {
                    'n' -> '\n'
                    'r' -> '\r'
                    't' -> '\t'
                    'b' -> '\b'
                    'f' -> '\u000c'
                    'u' -> String(CharArray(4) {
                        next()
                        ensure(char.isHexDigit()) { "Invalid hex digit" }
                        char
                    }).toInt(16).toChar()
                    else -> char
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
