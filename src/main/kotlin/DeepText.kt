package deep

import serial.*
import java.io.Reader
import java.io.StringWriter
import java.io.Writer

// Read API

public fun <T> Reader.readDeep(handler: DeepEvent.Handler<T>, readValue: CharCursor.() -> T): Unit = parse {
    skipWhitespace()
    readDeep(handler, readValue)
}

public fun <T> Reader.readDeep(readValue: CharCursor.() -> T): Deep<T> {
    val handler = DefaultHandler<T>()
    readDeep(handler, readValue)
    return handler.value ?: error("Expected end of input")
}

// Write API

public fun <T> Writer.writeDeep(value: Deep<T>, writeValue: Writer.(T) -> Unit): Unit =
    writeDeep(value, "  ", 0, writeValue)

public fun <T> Writer.writeDeepMinified(value: Deep<T>, writeValue: Writer.(T) -> Unit): Unit =
    writeDeep(value, null, 0, writeValue)

public fun <T> Writer.writeDeep(value: Deep<T>, indent: String, writeValue: Writer.(T) -> Unit): Unit =
    writeDeep(value, indent, 0, writeValue)

public fun <T> Deep<T>.toString(writeValue: Writer.(T) -> Unit): String =
    StringWriter().apply { use { it.writeDeep(this@toString, writeValue) } }.toString()

public fun <T> Deep<T>.toString(indent: String, writeValue: Writer.(T) -> Unit): String =
    StringWriter().apply { use { it.writeDeep(this@toString, indent, writeValue) } }.toString()

public fun <T> Deep<T>.toStringMinified(writeValue: Writer.(T) -> Unit): String =
    StringWriter().apply { use { it.writeDeepMinified(this@toStringMinified, writeValue) } }.toString()


// Read Helpers

private fun <T> CharCursor.readDeep(handler: DeepEvent.Handler<T>, readValue: CharCursor.() -> T): Unit =
    when (current) {
        '{' -> readMap(handler, readValue)
        '[' -> readList(handler, readValue)
        else -> readValue(handler, readValue)
    }

private fun <T> CharCursor.readMap(handler: DeepEvent.Handler<T>, readValue: CharCursor.() -> T) {
    handler.handle(DeepEvent.MapStart)
    readRequiredChar('{')
    readCollection('}') {
        val key = decodeStringLiteral()
        handler.handle(DeepEvent.Key(key))
        skipWhitespace()
        if (!readOptionalChar(':')) readRequiredChar('=')
        skipWhitespace()
        readDeep(handler, readValue)
    }
    handler.handle(DeepEvent.MapEnd)
}

private fun <T> CharCursor.readList(handler: DeepEvent.Handler<T>, readValue: CharCursor.() -> T) {
    handler.handle(DeepEvent.ListStart)
    readRequiredChar('[')
    readCollection(']') {
        readDeep(handler, readValue)
    }
    handler.handle(DeepEvent.ListEnd)
}

private fun <T> CharCursor.readValue(handler: DeepEvent.Handler<T>, readValue: CharCursor.() -> T) {
    handler.handle(DeepEvent.Value(readValue()))
}

public fun CharCursor.decodeStringLiteral(): String {
    val delimiter = read()
    ensure(delimiter == '"' || delimiter == '\'') { "Expected: \" or '" }
    val string = capturing {
        while (current != delimiter) {
            ensure(current >= '\u0020') { "Invalid character" }
            if (current == '\\') {
                notCapturing {
                    next()
                    this@capturing.capture(
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
                }
            } else {
                next()
            }
        }
    }
    readRequiredChar(delimiter)
    return string
}

public fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

private inline fun CharCursor.readCollection(end: Char, action: () -> Unit) {
    do {
        skipWhitespace()
        if (readOptionalChar(end)) return
        action()
        skipWhitespace()
    } while (readOptionalChar(','))
    readRequiredChar(end)
}

// Write Helpers

private fun <T> Writer.writeDeep(value: Deep<T>, indent: String?, level: Int, writeValue: Writer.(T) -> Unit) {
    when (value) {
        is DeepMap -> writeMap(value.data, indent, level, writeValue)
        is DeepList -> writeList(value.data, indent, level, writeValue)
        is DeepValue -> writeValue(value.data, writeValue)
    }
}

private fun <T> Writer.writeMap(
    map: Map<String, Deep<T>>,
    indent: String?,
    level: Int,
    writeValue: Writer.(T) -> Unit,
) {
    append('{')
    when (map.size) {
        0 -> Unit
        1 -> writeEntry(map.entries.first(), indent, level, writeValue)
        else -> {
            var index = 0
            for (entry in map) {
                if (index++ > 0) append(',')
                appendLineAndIndent(indent, level + 1)
                writeEntry(entry, indent, level + 1, writeValue)
            }
            appendLineAndIndent(indent, level)
        }
    }
    append('}')
}

private fun <T> Writer.writeList(
    list: List<Deep<T>>,
    indent: String?,
    level: Int,
    writeValue: Writer.(T) -> Unit,
) {
    append('[')
    when (list.size) {
        0 -> Unit
        1 -> writeDeep(list[0], indent, level, writeValue)
        else -> {
            list.forEachIndexed { i, v ->
                if (i > 0) append(',')
                if (list.size > 1) {
                    appendLineAndIndent(indent, level + 1)
                    writeDeep(v, indent, level + 1, writeValue)
                }
            }
            appendLineAndIndent(indent, level)
        }
    }
    append(']')
}

private fun <T> Writer.writeValue(value: T, writeValue: Writer.(T) -> Unit) = writeValue(value)

private fun <T> Writer.writeEntry(
    entry: Map.Entry<String, Deep<T>>,
    indent: String?,
    level: Int,
    writeValue: Writer.(T) -> Unit,
) {
    encodeStringLiteral(entry.key)
    append(':')
    if (indent != null) append(' ')
    writeDeep(entry.value, indent, level, writeValue)
}

private fun Writer.appendLineAndIndent(indent: String?, level: Int) {
    if (indent != null) {
        appendLine()
        repeat(level) { write(indent) }
    }
}

private const val HEX_CHARS = "0123456789abcdef"
public fun Writer.encodeStringLiteral(string: String) {
    append('"')
    for (char in string) {
        when {
            char == '\\' -> write("\\\\")
            char == '"' -> write("\\\"")
            char == '\n' -> write("\\n")
            char == '\r' -> write("\\r")
            char == '\t' -> write("\\t")
            char < '\u0020' -> {
                write("\\u00")
                val code = char.code
                append(HEX_CHARS[code shr 4 and 0xf])
                append(HEX_CHARS[code and 0xf])
            }

            else -> append(char)
        }
    }
    append('"')
}
