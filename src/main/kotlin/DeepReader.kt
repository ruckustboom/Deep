package deep

import serial.*
import java.io.Reader

public fun <T> Reader.readDeep(handler: DeepEvent.Handler<T>, reader: ValueReader<T>): Unit = parse {
    skipWhitespace()
    readDeep(handler, reader)
}

public fun <T> Reader.readDeep(reader: ValueReader<T>): Deep<T> {
    val handler = DefaultHandler<T>()
    readDeep(handler, reader)
    return handler.value ?: error("Expected end of input")
}

public fun interface ValueReader<out T> {
    public fun TextParseState.readValue(): T
}

// Helpers

private fun <T> TextParseState.readDeep(handler: DeepEvent.Handler<T>, parser: ValueReader<T>): Unit = when (current) {
    '{' -> readMap(handler, parser)
    '[' -> readList(handler, parser)
    else -> readValue(handler, parser)
}

private fun <T> TextParseState.readMap(handler: DeepEvent.Handler<T>, parser: ValueReader<T>) {
    handler.handle(DeepEvent.MapStart)
    parseCollection('}') {
        val key = decodeStringLiteral()
        handler.handle(DeepEvent.Key(key))
        skipWhitespace()
        if (!readOptionalChar(':')) readRequiredChar('=')
        skipWhitespace()
        readDeep(handler, parser)
    }
    handler.handle(DeepEvent.MapEnd)
}

private fun <T> TextParseState.readList(handler: DeepEvent.Handler<T>, parser: ValueReader<T>) {
    handler.handle(DeepEvent.ListStart)
    parseCollection(']') {
        readDeep(handler, parser)
    }
    handler.handle(DeepEvent.ListEnd)
}

private fun <T> TextParseState.readValue(handler: DeepEvent.Handler<T>, parser: ValueReader<T>) = with(parser) {
    handler.handle(DeepEvent.Value(readValue()))
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
