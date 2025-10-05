package deep.common

import serial.ByteCursor
import serial.CharCursor
import serial.read
import serial.readLiteral
import serial.writeByte
import java.io.OutputStream

public inline fun <T : Any> ByteCursor.readNullable(readValue: ByteCursor.() -> T): T? =
    if (read() == 0.toByte()) null else readValue()

public inline fun <T: Any> OutputStream.writeNullable(value: T?, writeValue: OutputStream.(T) -> Unit) {
    if (value == null) {
        writeByte(0)
    } else {
        writeByte(1)
        writeValue(value)
    }
}

public inline fun <T: Any> CharCursor.readNullable(
    nullValue: String = "null",
    ignoreCase: Boolean = false,
    readValue: CharCursor.() -> T
): T? {
    require(nullValue.isNotEmpty()) { "Null value must not be an empty string" }
    require(nullValue.trim() == nullValue) { "Null value must not have surrounding whitespace" }
    return if (current == nullValue.first()) {
        readLiteral(nullValue, ignoreCase)
        null
    } else {
        readValue()
    }
}

public inline fun <T: Any> Appendable.writeNullable(
    value: T?,
    nullValue: String = "null",
    writeValue: Appendable.(T) -> Unit,
) {
    require(nullValue.isNotEmpty()) { "Null value must not be an empty string" }
    require(nullValue.trim() == nullValue) { "Null value must not have surrounding whitespace" }
    if (value == null) {
        append(nullValue)
    } else {
        writeValue(value)
    }
}
