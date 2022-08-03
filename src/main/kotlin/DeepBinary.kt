package deep

import serial.*
import java.io.InputStream
import java.io.OutputStream

// Parse API

private enum class DeepType { MAP, LIST, VALUE }

public fun <T> InputStream.parseDeep(handler: DeepEvent.Handler<T>, parse: InputStream.() -> T): Unit =
    when (readEnumByOrdinalByte<DeepType>()) {
        DeepType.MAP -> parseMap(handler, parse)
        DeepType.LIST -> parseList(handler, parse)
        DeepType.VALUE -> parseValue(handler, parse)
    }

public fun <T> InputStream.parseDeep(parse: InputStream.() -> T): Deep<T> {
    val handler = DefaultHandler<T>()
    parseDeep(handler, parse)
    return handler.value ?: error("Expected end of input")
}

// Serialize API

public fun <T> Deep<T>.toByteArray(serialize: OutputStream.(T) -> Unit): ByteArray =
    makeByteArray { writeDeep(this@toByteArray, serialize) }

public fun <T> OutputStream.writeDeep(deep: Deep<T>, serialize: OutputStream.(T) -> Unit) {
    when (deep) {
        is DeepMap -> {
            writeEnumByOrdinalByte(DeepType.MAP)
            writeMap(deep.data) { k, v ->
                writeString(k)
                writeDeep(v, serialize)
            }
        }
        is DeepList -> {
            writeEnumByOrdinalByte(DeepType.LIST)
            writeValues(deep.data) { writeDeep(it, serialize) }
        }
        is DeepValue -> {
            writeEnumByOrdinalByte(DeepType.VALUE)
            serialize(deep.data)
        }
    }
}

// Parse Helpers

private fun <T> InputStream.parseMap(handler: DeepEvent.Handler<T>, parse: InputStream.() -> T) {
    handler.handle(DeepEvent.MapStart)
    val entries = readInt()
    repeat(entries) {
        handler.handle(DeepEvent.Key(readString()))
        parseDeep(handler, parse)
    }
    handler.handle(DeepEvent.MapEnd)
}

private fun <T> InputStream.parseList(handler: DeepEvent.Handler<T>, parse: InputStream.() -> T) {
    handler.handle(DeepEvent.ListStart)
    val entries = readInt()
    repeat(entries) { parseDeep(handler, parse) }
    handler.handle(DeepEvent.ListEnd)
}

private fun <T> InputStream.parseValue(handler: DeepEvent.Handler<T>, parse: InputStream.() -> T) =
    handler.handle(DeepEvent.Value(parse()))
