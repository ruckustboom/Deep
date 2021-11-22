package deep

import java.io.StringWriter
import java.io.Writer

private typealias Entry<T> = Map.Entry<String, Deep<T>>

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

// Helpers

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

private fun <T> Writer.writeEntry(entry: Entry<T>, indent: String?, level: Int, writer: ValueWriter<T>) {
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
