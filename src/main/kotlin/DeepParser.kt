package deep

import serial.readByte
import serial.readInt
import serial.readString
import java.io.InputStream

public fun <T> InputStream.parseDeep(handler: DeepEvent.Handler<T>, parser: ValueParser<T>): Unit = when (readByte()) {
    3.toByte() -> parseMap(handler, parser)
    2.toByte() -> parseList(handler, parser)
    1.toByte() -> parseValue(handler, parser)
    else -> error("Invalid")
}

public fun <T> InputStream.parseDeep(parser: ValueParser<T>): Deep<T> {
    val handler = DefaultHandler<T>()
    parseDeep(handler, parser)
    return handler.value ?: error("Expected end of input")
}

public fun interface ValueParser<out T> {
    public fun InputStream.parseValue(): T
}

// Helpers

private fun <T> InputStream.parseMap(handler: DeepEvent.Handler<T>, parser: ValueParser<T>) {
    handler.handle(DeepEvent.MapStart)
    val entries = readInt()
    repeat(entries) {
        handler.handle(DeepEvent.Key(readString()))
        parseDeep(handler, parser)
    }
    handler.handle(DeepEvent.MapEnd)
}

private fun <T> InputStream.parseList(handler: DeepEvent.Handler<T>, parser: ValueParser<T>) {
    handler.handle(DeepEvent.ListStart)
    val entries = readInt()
    repeat(entries) { parseDeep(handler, parser) }
    handler.handle(DeepEvent.ListEnd)
}

private fun <T> InputStream.parseValue(handler: DeepEvent.Handler<T>, parser: ValueParser<T>) = with(parser) {
    handler.handle(DeepEvent.Value(parseValue()))
}
