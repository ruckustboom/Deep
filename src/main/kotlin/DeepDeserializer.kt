package deep

import java.io.Reader
import java.io.StringReader

public fun interface ValueDeserializer<out T> {
    public fun TextParseState.deserializeValue(): T
}

public fun <T> Reader.deserializeDeep(deserializer: ValueDeserializer<T>): Deep<T> = parse {
    skipWhitespace()
    return deserializeDeep(deserializer)
}

private fun <T> TextParseState.deserializeDeep(deserializer: ValueDeserializer<T>): Deep<T> = when (char) {
    '{' -> deserializeMap(deserializer)
    '[' -> deserializeList(deserializer)
    else -> DeepValue(with(deserializer) { deserializeValue() })
}

private fun <T> TextParseState.deserializeMap(deserializer: ValueDeserializer<T>): DeepMap<T> {
    val map = mutableMapOf<String, Deep<T>>()
    deserializeCollection('}') {
        val key = decodeStringLiteral()
        skipWhitespace()
        if (!readOptionalChar(':')) readRequiredChar('=')
        skipWhitespace()
        map[key] = deserializeDeep(deserializer)
    }
    return DeepMap(map)
}

private fun <T> TextParseState.deserializeList(deserializer: ValueDeserializer<T>): DeepList<T> {
    val list = mutableListOf<Deep<T>>()
    deserializeCollection(']') {
        list += deserializeDeep(deserializer)
    }
    return DeepList(list)
}

// TODO: Allow trailing commas
private inline fun TextParseState.deserializeCollection(end: Char, action: () -> Unit) {
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

public fun TextParseState.skipWhitespace(): Unit = readWhile { it.isWhitespace() }

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

public fun TextParseState.readRequiredChar(char: Char): Unit = ensure(readOptionalChar(char)) { "Expected: $char" }
public fun TextParseState.readOptionalChar(char: Char): Boolean = readIf { it.toLowerCase() == char.toLowerCase() }
public fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
