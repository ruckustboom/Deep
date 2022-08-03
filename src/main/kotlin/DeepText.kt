package deep

import serial.*
import java.io.Reader
import java.io.StringWriter
import java.io.Writer

// Read API

public fun <T> Reader.readDeep(handler: DeepEvent.Handler<T>, read: CharCursor.() -> T): Unit = parse {
    skipWhitespace()
    readDeep(handler, read)
}

public fun <T> Reader.readDeep(read: CharCursor.() -> T): Deep<T> {
    val handler = DefaultHandler<T>()
    readDeep(handler, read)
    return handler.value ?: error("Expected end of input")
}

// Write API

public fun <T> Writer.writeDeep(value: Deep<T>, write: Writer.(T) -> Unit): Unit =
    writeDeep(value, "  ", 0, write)

public fun <T> Writer.writeDeepMinified(value: Deep<T>, write: Writer.(T) -> Unit): Unit =
    writeDeep(value, null, 0, write)

public fun <T> Writer.writeDeep(value: Deep<T>, indent: String, write: Writer.(T) -> Unit): Unit =
    writeDeep(value, indent, 0, write)

public fun <T> Deep<T>.toString(write: Writer.(T) -> Unit): String =
    StringWriter().apply { use { it.writeDeep(this@toString, write) } }.toString()

public fun <T> Deep<T>.toString(indent: String, write: Writer.(T) -> Unit): String =
    StringWriter().apply { use { it.writeDeep(this@toString, indent, write) } }.toString()

public fun <T> Deep<T>.toStringMinified(write: Writer.(T) -> Unit): String =
    StringWriter().apply { use { it.writeDeepMinified(this@toStringMinified, write) } }.toString()


// Read Helpers

private fun <T> CharCursor.readDeep(handler: DeepEvent.Handler<T>, read: CharCursor.() -> T): Unit =
    when (current) {
        '{' -> readMap(handler, read)
        '[' -> readList(handler, read)
        else -> readValue(handler, read)
    }

private fun <T> CharCursor.readMap(handler: DeepEvent.Handler<T>, read: CharCursor.() -> T) {
    handler.handle(DeepEvent.MapStart)
    readCollection('}') {
        val key = decodeStringLiteral()
        handler.handle(DeepEvent.Key(key))
        skipWhitespace()
        if (!readOptionalChar(':')) readRequiredChar('=')
        skipWhitespace()
        readDeep(handler, read)
    }
    handler.handle(DeepEvent.MapEnd)
}

private fun <T> CharCursor.readList(handler: DeepEvent.Handler<T>, read: CharCursor.() -> T) {
    handler.handle(DeepEvent.ListStart)
    readCollection(']') {
        readDeep(handler, read)
    }
    handler.handle(DeepEvent.ListEnd)
}

private fun <T> CharCursor.readValue(handler: DeepEvent.Handler<T>, read: CharCursor.() -> T) {
    handler.handle(DeepEvent.Value(read()))
}

public fun CharCursor.decodeStringLiteral(): String {
    val delimiter = current
    ensure(delimiter == '"' || delimiter == '\'') { "Expected: \" or '" }
    next()
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
    ensure(current == delimiter) { "Expected: $delimiter" }
    next()
    return string
}

public fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

private inline fun CharCursor.readCollection(end: Char, action: () -> Unit) {
    next()
    do {
        skipWhitespace()
        if (readOptionalChar(end)) return
        action()
        skipWhitespace()
    } while (readOptionalChar(','))
    readRequiredChar(end)
}

// Write Helpers

private fun <T> Writer.writeDeep(value: Deep<T>, indent: String?, level: Int, write: Writer.(T) -> Unit) {
    when (value) {
        is DeepMap -> writeMap(value.data, indent, level, write)
        is DeepList -> writeList(value.data, indent, level, write)
        is DeepValue -> writeValue(value.data, write)
    }
}

private fun <T> Writer.writeMap(map: Map<String, Deep<T>>, indent: String?, level: Int, write: Writer.(T) -> Unit) {
    append('{')
    when (map.size) {
        0 -> Unit
        1 -> writeEntry(map.entries.first(), indent, level, write)
        else -> {
            var index = 0
            for (entry in map) {
                if (index++ > 0) append(',')
                appendLineAndIndent(indent, level + 1)
                writeEntry(entry, indent, level + 1, write)
            }
            appendLineAndIndent(indent, level)
        }
    }
    append('}')
}

private fun <T> Writer.writeList(list: List<Deep<T>>, indent: String?, level: Int, write: Writer.(T) -> Unit) {
    append('[')
    when (list.size) {
        0 -> Unit
        1 -> writeDeep(list[0], indent, level, write)
        else -> {
            list.forEachIndexed { i, v ->
                if (i > 0) append(',')
                if (list.size > 1) {
                    appendLineAndIndent(indent, level + 1)
                    writeDeep(v, indent, level + 1, write)
                }
            }
            appendLineAndIndent(indent, level)
        }
    }
    append(']')
}

private fun <T> Writer.writeValue(value: T, write: Writer.(T) -> Unit) = write(value)

private fun <T> Writer.writeEntry(
    entry: Map.Entry<String, Deep<T>>,
    indent: String?,
    level: Int,
    write: Writer.(T) -> Unit,
) {
    encodeStringLiteral(entry.key)
    append(':')
    if (indent != null) append(' ')
    writeDeep(entry.value, indent, level, write)
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
