package deep.v2

import java.io.StringWriter
import java.io.Writer

public fun interface ValueWriter<T> {
    public fun Writer.append(value: T)
}

public class DeepFormatter<T> private constructor(
    private val valueWriter: ValueWriter<T>,
    private val indentation: String?,
) {
    public fun format(value: Deep<T>, writer: Writer) {
        writer.appendDeep(value, 0)
    }

    private fun Writer.appendDeep(value: Deep<T>, level: Int) {
        when (value) {
            is DeepMap -> appendMap(value.map, level)
            is DeepList -> appendList(value.list, level)
            is DeepValue -> with(valueWriter) { append(value.value) }
        }
    }

    private fun Writer.appendMap(map: Map<String, Deep<T>>, level: Int) {
        append('{')
        when (map.size) {
            0 -> Unit
            1 -> appendEntry(map.entries.first(), level)
            else -> {
                var index = 0
                for (entry in map) {
                    if (index++ > 0) append(',')
                    if (indentation != null) appendLine()
                    appendIndent(level + 1)
                    appendEntry(entry, level + 1)
                }
                if (indentation != null) appendLine()
                appendIndent(level)
            }
        }
        append('}')
    }

    private fun Writer.appendEntry(entry: Map.Entry<String, Deep<T>>, level: Int) {
        encode(entry.key)
        append(':')
        if (indentation != null) append(' ')
        appendDeep(entry.value, level)
    }

    private fun Writer.appendList(list: List<Deep<T>>, level: Int) {
        append('[')
        when (list.size) {
            0 -> Unit
            1 -> appendDeep(list[0], level)
            else -> {
                list.forEachIndexed { i, v ->
                    if (i > 0) append(',')
                    if (list.size > 1) {
                        if (indentation != null) appendLine()
                        appendIndent(level + 1)
                        appendDeep(v, level + 1)
                    }
                }
                if (indentation != null) appendLine()
                appendIndent(level)
            }
        }
        append(']')
    }

    private fun Writer.appendIndent(level: Int) {
        if (indentation != null) repeat(level) { write(indentation) }
    }

    public companion object {
        public fun <T> tabs(valueWriter: ValueWriter<T>): DeepFormatter<T> = DeepFormatter(valueWriter, "\t")
        public fun <T> spaces(count: Int = 2, writer: ValueWriter<T>): DeepFormatter<T> = spaces(writer, count)
        public fun <T> spaces(valueWriter: ValueWriter<T>, count: Int = 2): DeepFormatter<T> =
            DeepFormatter(valueWriter, " ".repeat(count))

        public fun <T> minified(valueWriter: ValueWriter<T>): DeepFormatter<T> = DeepFormatter(valueWriter, null)
    }
}

public fun <T> DeepFormatter<T>.toString(value: Deep<T>): String {
    val writer = StringWriter()
    writer.use { format(value, it) }
    return writer.toString()
}

private const val HEX_CHARS = "0123456789abcdef"
public fun Writer.encode(string: String) {
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
                val int = char.toInt()
                append(HEX_CHARS[int shr 4 and 0xf])
                append(HEX_CHARS[int and 0xf])
            }
            else -> append(char)
        }
    }
    append('"')
}
