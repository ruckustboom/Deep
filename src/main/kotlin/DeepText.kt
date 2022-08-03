package deep

import serial.*
import java.io.Reader
import java.io.StringWriter
import java.io.Writer

// Read API

public fun interface ValueReader<out T> {
    public fun TextParseState.readValue(): T
}

public fun <T> Reader.readDeep(handler: DeepEvent.Handler<T>, reader: ValueReader<T>): Unit = parse {
    skipWhitespace()
    readDeep(handler, reader)
}

public fun <T> Reader.readDeep(reader: ValueReader<T>): Deep<T> {
    val handler = DefaultHandler<T>()
    readDeep(handler, reader)
    return handler.value ?: error("Expected end of input")
}

// Write API

public fun interface ValueWriter<in T> {
    public fun Writer.writeValue(value: T)
}

public fun <T> Writer.writeDeep(value: Deep<T>, writer: ValueWriter<T>): Unit =
    writeDeep(value, "  ", 0, writer)

public fun <T> Writer.writeDeepMinified(value: Deep<T>, writer: ValueWriter<T>): Unit =
    writeDeep(value, null, 0, writer)

public fun <T> Writer.writeDeep(value: Deep<T>, indent: String, writer: ValueWriter<T>): Unit =
    writeDeep(value, indent, 0, writer)

public fun <T> Deep<T>.toString(writer: ValueWriter<T>): String =
    StringWriter().apply { use { it.writeDeep(this@toString, writer) } }.toString()

public fun <T> Deep<T>.toString(indent: String, writer: ValueWriter<T>): String =
    StringWriter().apply { use { it.writeDeep(this@toString, indent, writer) } }.toString()

public fun <T> Deep<T>.toStringMinified(writer: ValueWriter<T>): String =
    StringWriter().apply { use { it.writeDeepMinified(this@toStringMinified, writer) } }.toString()


// Read Helpers

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
    val string = capturing {
        while (current != delimiter) {
            ensure(current >= '\u0020') { "Invalid character" }
            if (current == '\\') {
                notCapturing {
                    next()
                    capture(
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

// Write Helpers

private fun <T> Writer.writeDeep(value: Deep<T>, indent: String?, level: Int, writer: ValueWriter<T>) {
    when (value) {
        is DeepMap -> writeMap(value.data, indent, level, writer)
        is DeepList -> writeList(value.data, indent, level, writer)
        is DeepValue -> writeValue(value.data, writer)
    }
}

private fun <T> Writer.writeMap(map: Map<String, Deep<T>>, indent: String?, level: Int, writer: ValueWriter<T>) {
    append('{')
    when (map.size) {
        0 -> Unit
        1 -> writeEntry(map.entries.first(), indent, level, writer)
        else -> {
            var index = 0
            for (entry in map) {
                if (index++ > 0) append(',')
                appendLineAndIndent(indent, level + 1)
                writeEntry(entry, indent, level + 1, writer)
            }
            appendLineAndIndent(indent, level)
        }
    }
    append('}')
}

private fun <T> Writer.writeList(list: List<Deep<T>>, indent: String?, level: Int, writer: ValueWriter<T>) {
    append('[')
    when (list.size) {
        0 -> Unit
        1 -> writeDeep(list[0], indent, level, writer)
        else -> {
            list.forEachIndexed { i, v ->
                if (i > 0) append(',')
                if (list.size > 1) {
                    appendLineAndIndent(indent, level + 1)
                    writeDeep(v, indent, level + 1, writer)
                }
            }
            appendLineAndIndent(indent, level)
        }
    }
    append(']')
}

private fun <T> Writer.writeValue(value: T, writer: ValueWriter<T>) {
    with(writer) { writeValue(value) }
}

private fun <T> Writer.writeEntry(
    entry: Map.Entry<String, Deep<T>>,
    indent: String?,
    level: Int,
    writer: ValueWriter<T>,
) {
    encodeStringLiteral(entry.key)
    append(':')
    if (indent != null) append(' ')
    writeDeep(entry.value, indent, level, writer)
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
