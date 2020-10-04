package deep

import java.io.Reader
import java.io.StringReader

public fun interface ValueParser<out T> {
    public fun ParseState.parseValue(): T
}

public fun <T> Reader.parseDeep(parser: ValueParser<T>): Deep<T> = parse {
    skipWhitespace()
    return parseDeep(parser)
}

public inline fun <T> String.parseDeep(parser: ValueParser<T>): Deep<T> =
    StringReader(this).use { it.parseDeep(parser) }

private fun ParseState.skipWhitespace() = readWhile { it.isWhitespace() }

private fun <T> ParseState.parseDeep(parser: ValueParser<T>): Deep<T> = when (char) {
    '{' -> parseMap(parser)
    '[' -> parseList(parser)
    else -> DeepValue(with(parser) { parseValue() })
}

private fun <T> ParseState.parseMap(parser: ValueParser<T>): DeepMap<T> {
    val map = mutableMapOf<String, Deep<T>>()
    parseCollection('}') {
        val key = parseStringLiteral()
        skipWhitespace()
        if (!readOptionalChar(':')) readRequiredChar('=')
        skipWhitespace()
        map[key] = parseDeep(parser)
    }
    return DeepMap(map)
}

private fun <T> ParseState.parseList(parser: ValueParser<T>): DeepList<T> {
    val list = mutableListOf<Deep<T>>()
    parseCollection(']') {
        list += parseDeep(parser)
    }
    return DeepList(list)
}

private inline fun ParseState.parseCollection(end: Char, action: () -> Unit) {
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

// Helpers

public fun ParseState.parseStringLiteral(): String {
    val delimiter = char
    ensure(delimiter == '"' || delimiter == '\'') { "Expected: \" or '" }
    readRequiredChar(delimiter)
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

public fun ParseState.readRequiredChar(char: Char): Unit = ensure(readOptionalChar(char)) { "Expected: $char" }
public fun ParseState.readOptionalChar(char: Char): Boolean = readIf { it.toLowerCase() == char.toLowerCase() }
public fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
