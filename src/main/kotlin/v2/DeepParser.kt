package deep.v2

import java.io.Reader

public fun interface ValueParser<T> {
    public fun ParseState.parse(): T
}

public fun <T> Reader.parseDeep(parser: ValueParser<T>): Deep<T> = parse {
    skipWhitespace()
    return readDeep(parser)
}

private fun ParseState.skipWhitespace() = readWhile { it.isWhitespace() }

private fun <T> ParseState.readDeep(parser: ValueParser<T>): Deep<T> = when (char) {
    '{' -> readMap(parser)
    '[' -> readList(parser)
    else -> DeepValue(with(parser) { parse() })
}

private fun <T> ParseState.readMap(parser: ValueParser<T>): DeepMap<T> {
    val map = DeepMap<T>()
    readCollection('}') {
        val key = readStringLiteral()
        skipWhitespace()
        readRequiredChar(':')
        skipWhitespace()
        map[key] = readDeep(parser)
    }
    return map
}

private fun<T> ParseState.readList(parser: ValueParser<T>): DeepList<T> {
    val list = DeepList<T>()
    readCollection(']') {
        list += readDeep(parser)
    }
    return list
}

private inline fun ParseState.readCollection(end: Char, action: () -> Unit) {
    read()
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

private fun ParseState.readStringLiteral(): String {
    val delim = char
    ensure(delim == '"' || delim == '\'') { "Expected: \" or '" }
    readRequiredChar(delim)
    startCapture()
    while (char != delim) {
        ensure(char >= '\u0020') { "Invalid character" }
        if (char == '\\') {
            pauseCapture()
            read()
            addToCapture(
                when (char) {
                    'n' -> '\n'
                    'r' -> '\r'
                    't' -> '\t'
                    'b' -> '\b'
                    'f' -> '\u000c'
                    'u' -> String(CharArray(4) {
                        read()
                        ensure(char.isHexDigit()) { "Invalid hex digit" }
                        char
                    }).toInt(16).toChar()
                    else -> char
                }
            )
            read()
            startCapture()
        } else {
            read()
        }
    }
    val string = finishCapture()
    read()
    return string
}

public fun ParseState.readRequiredChar(char: Char): Unit = ensure(readOptionalChar(char)) { "Expected: $char" }
public fun ParseState.readOptionalChar(char: Char): Boolean = readIf { it.toLowerCase() == char.toLowerCase() }
public fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
