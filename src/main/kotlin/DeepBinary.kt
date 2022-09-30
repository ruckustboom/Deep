package deep

import serial.*
import java.io.InputStream
import java.io.OutputStream

// Parse API

private enum class DeepType { MAP, LIST, VALUE }

public fun <T> InputStream.parseDeep(handler: DeepEvent.Handler<T>, readValue: ByteCursor.() -> T): Unit =
    parse { readDeep(handler, readValue) }

public fun <T> InputStream.parseDeep(readValue: ByteCursor.() -> T): Deep<T> {
    val handler = DefaultHandler<T>()
    parseDeep(handler, readValue)
    return handler.value ?: error("Expected end of input")
}

// Serialize API

public fun <T> Deep<T>.toByteArray(writeValue: OutputStream.(T) -> Unit): ByteArray =
    makeByteArray { writeDeep(this@toByteArray, writeValue) }

public fun <T> OutputStream.writeDeep(deep: Deep<T>, writeValue: OutputStream.(T) -> Unit) {
    when (deep) {
        is DeepMap -> {
            writeEnumByOrdinalByte(DeepType.MAP)
            writeMap(deep.data) { k, v ->
                writeString(k)
                writeDeep(v, writeValue)
            }
        }

        is DeepList -> {
            writeEnumByOrdinalByte(DeepType.LIST)
            writeValues(deep.data) { writeDeep(it, writeValue) }
        }

        is DeepValue -> {
            writeEnumByOrdinalByte(DeepType.VALUE)
            writeValue(deep.data)
        }
    }
}

// Parse Helpers

private fun <T> ByteCursor.readDeep(handler: DeepEvent.Handler<T>, readValue: ByteCursor.() -> T): Unit =
    when (readEnumByOrdinalByte<DeepType>()) {
        DeepType.MAP -> readMap(handler, readValue)
        DeepType.LIST -> readList(handler, readValue)
        DeepType.VALUE -> readValue(handler, readValue)
    }

private fun <T> ByteCursor.readMap(handler: DeepEvent.Handler<T>, readValue: ByteCursor.() -> T) {
    handler.handle(DeepEvent.MapStart)
    repeatRead {
        handler.handle(DeepEvent.Key(readString()))
        readDeep(handler, readValue)
    }
    handler.handle(DeepEvent.MapEnd)
}

private fun <T> ByteCursor.readList(handler: DeepEvent.Handler<T>, readValue: ByteCursor.() -> T) {
    handler.handle(DeepEvent.ListStart)
    repeatRead { readDeep(handler, readValue) }
    handler.handle(DeepEvent.ListEnd)
}

private fun <T> ByteCursor.readValue(handler: DeepEvent.Handler<T>, readValue: ByteCursor.() -> T) =
    handler.handle(DeepEvent.Value(readValue()))
