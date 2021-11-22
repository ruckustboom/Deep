package deep

import serial.*
import java.io.OutputStream

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

public fun interface ValueSerializer<in T> {
    public fun OutputStream.serializeValue(value: T)
}
