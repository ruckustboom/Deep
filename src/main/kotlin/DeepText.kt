package deep

import serial.*
import java.io.Reader

// Read API

public fun <T> Reader.parseDeep(handler: DeepEvent.Handler<T>, readValue: CharCursor.() -> T): Unit = parse {
    readWhileWhitespace()
    readDeep(handler, readValue)
}

public fun <T> Reader.parseDeep(readValue: CharCursor.() -> T): Deep<T> {
    val handler = DefaultHandler<T>()
    parseDeep(handler, readValue)
    return handler.value ?: error("Expected end of input")
}

public fun <T> String.parseDeep(handler: DeepEvent.Handler<T>, readValue: CharCursor.() -> T): Unit = parse {
    readWhileWhitespace()
    readDeep(handler, readValue)
}

public fun <T> String.parseDeep(readValue: CharCursor.() -> T): Deep<T> {
    val handler = DefaultHandler<T>()
    parseDeep(handler, readValue)
    return handler.value ?: error("Expected end of input")
}

// Write API

public fun <T> Appendable.writeDeep(value: Deep<T>, writeValue: Appendable.(T) -> Unit): Unit =
    writeDeep(value, "  ", 0, writeValue)

public fun <T> Appendable.writeDeepMinified(value: Deep<T>, writeValue: Appendable.(T) -> Unit): Unit =
    writeDeep(value, null, 0, writeValue)

public fun <T> Appendable.writeDeep(value: Deep<T>, indent: String, writeValue: Appendable.(T) -> Unit): Unit =
    writeDeep(value, indent, 0, writeValue)

public fun <T> Deep<T>.toString(writeValue: Appendable.(T) -> Unit): String =
    buildString { writeDeep(this@toString, writeValue) }

public fun <T> Deep<T>.toString(indent: String, writeValue: Appendable.(T) -> Unit): String =
    buildString { writeDeep(this@toString, indent, writeValue) }

public fun <T> Deep<T>.toStringMinified(writeValue: Appendable.(T) -> Unit): String =
    buildString { writeDeepMinified(this@toStringMinified, writeValue) }


// Read Helpers

private fun <T> CharCursor.readDeep(
    handler: DeepEvent.Handler<T>,
    readValue: CharCursor.() -> T,
): Unit = when (current) {
    '{' -> readMap(handler, readValue)
    '[' -> readList(handler, readValue)
    else -> readValue(handler, readValue)
}

private fun <T> CharCursor.readMap(handler: DeepEvent.Handler<T>, readValue: CharCursor.() -> T) {
    handler.handle(DeepEvent.MapStart)
    readRequiredChar('{')
    readCollection('}') {
        ensure(current == '"' || current == '\'') { "Expected \" or '" }
        val key = captureStringLiteral(current)
        handler.handle(DeepEvent.Key(key))
        readWhileWhitespace()
        if (!readOptionalChar(':')) readRequiredChar('=')
        readWhileWhitespace()
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

private inline fun CharCursor.readCollection(end: Char, action: () -> Unit) {
    do {
        readWhileWhitespace()
        if (readOptionalChar(end)) return
        action()
        readWhileWhitespace()
    } while (readOptionalChar(','))
    readRequiredChar(end)
}

// Write Helpers

private fun <T> Appendable.writeDeep(
    value: Deep<T>,
    indent: String?,
    level: Int,
    writeValue: Appendable.(T) -> Unit,
) = when (value) {
    is DeepMap -> writeMap(value.data, indent, level, writeValue)
    is DeepList -> writeList(value.data, indent, level, writeValue)
    is DeepValue -> writeValue(value.data, writeValue)
}

private fun <T> Appendable.writeMap(
    map: Map<String, Deep<T>>,
    indent: String?,
    level: Int,
    writeValue: Appendable.(T) -> Unit,
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

private fun <T> Appendable.writeList(
    list: List<Deep<T>>,
    indent: String?,
    level: Int,
    writeValue: Appendable.(T) -> Unit,
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

private fun <T> Appendable.writeValue(value: T, writeValue: Appendable.(T) -> Unit) = writeValue(value)

private fun <T> Appendable.writeEntry(
    entry: Map.Entry<String, Deep<T>>,
    indent: String?,
    level: Int,
    writeValue: Appendable.(T) -> Unit,
) {
    writeEncodedString(entry.key)
    append(':')
    if (indent != null) append(' ')
    writeDeep(entry.value, indent, level, writeValue)
}

private fun Appendable.appendLineAndIndent(indent: String?, level: Int) {
    if (indent != null) {
        appendLine()
        repeat(level) { append(indent) }
    }
}

private const val HEX_CHARS = "0123456789abcdef"
public fun Appendable.writeEncodedString(string: String) {
    append('"')
    for (char in string) {
        when {
            char == '\\' -> append("\\\\")
            char == '"' -> append("\\\"")
            char == '\n' -> append("\\n")
            char == '\r' -> append("\\r")
            char == '\t' -> append("\\t")
            char < '\u0020' -> {
                append("\\u00")
                val code = char.code
                append(HEX_CHARS[code shr 4 and 0xf])
                append(HEX_CHARS[code and 0xf])
            }

            else -> append(char)
        }
    }
    append('"')
}
