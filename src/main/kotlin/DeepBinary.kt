package deep

import serial.*
import java.io.InputStream
import java.io.OutputStream

// Parse API

public fun interface ValueParser<out T> {
    public fun InputStream.parseValue(): T
}

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

// Serialize API

public fun interface ValueSerializer<in T> {
    public fun OutputStream.serializeValue(value: T)
}

public fun <T> Deep<T>.toByteArray(compress: Boolean, serializer: ValueSerializer<T>): ByteArray =
    makeByteArray(compress) { writeDeep(this@toByteArray, serializer) }

public fun <T> OutputStream.writeDeep(deep: Deep<T>, serializer: ValueSerializer<T>) {
    when (deep) {
        is DeepMap -> {
            writeByte(3)
            writeMap(deep.data) { k, v ->
                writeString(k)
                writeDeep(v, serializer)
            }
        }
        is DeepList -> {
            writeByte(2)
            writeValues(deep.data) { writeDeep(it, serializer) }
        }
        is DeepValue -> {
            writeByte(1)
            with(serializer) { serializeValue(deep.data) }
        }
    }
}

// Parse Helpers

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
