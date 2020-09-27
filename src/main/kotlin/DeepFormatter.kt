@file:Suppress("NOTHING_TO_INLINE")

package deep

import java.io.StringWriter
import java.io.Writer

public class DeepFormatter private constructor(private val indentation: String?) {
    public fun format(value: Deep<*>?, writer: Writer) {
        writer.appendValue(value, 0)
    }

    private fun Writer.appendValue(value: Deep<*>?, level: Int) {
        when (value) {
            null -> append('/')
            is DeepString -> encode(value.value)
            is DeepList -> appendList(value, level)
            is DeepMap -> appendMap(value, level)
        }
    }

    private fun Writer.appendList(value: DeepList, level: Int) {
        val list = value.value
        append('[')
        when (list.size) {
            0 -> Unit
            1 -> appendValue(list[0], level)
            else -> {
                list.forEachIndexed { i, v ->
                    if (i > 0) append(',')
                    if (list.size > 1) {
                        if (indentation != null) appendLine()
                        appendIndent(level + 1)
                        appendValue(v, level + 1)
                    }
                }
                if (indentation != null) appendLine()
                appendIndent(level)
            }
        }
        append(']')
    }

    private fun Writer.appendMap(value: DeepMap, level: Int) {
        val map = value.value
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

    private fun Writer.appendEntry(entry: Map.Entry<String, Deep<*>?>, level: Int) {
        encode(entry.key)
        append(':')
        if (indentation != null) append(' ')
        appendValue(entry.value, level)
    }

    private fun Writer.appendIndent(level: Int) {
        if (indentation != null) repeat(level) { write(indentation) }
    }

    public companion object {
        public val TABS: DeepFormatter = DeepFormatter("\t")
        public val MINIFIED: DeepFormatter = DeepFormatter(null)
        public val SPACES: DeepFormatter = DeepFormatter("  ")

        public fun toString(value: Deep<*>?, formatter: DeepFormatter): String = formatter.toString(value)

        private const val HEX_CHARS = "0123456789abcdef"
        private fun Writer.encode(string: String) {
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
    }
}

@JvmSynthetic
public inline fun DeepFormatter.toString(value: Deep<*>?): String {
    val writer = StringWriter()
    writer.use { format(value, it) }
    return writer.toString()
}

@JvmSynthetic
public inline operator fun DeepFormatter.invoke(value: Deep<*>?): String = toString(value)

@JvmSynthetic
public inline operator fun DeepFormatter.invoke(value: Deep<*>?, writer: Writer): Unit = format(value, writer)

@JvmSynthetic
public inline fun Writer.write(value: Deep<*>?, formatter: DeepFormatter = DeepFormatter.MINIFIED): Unit =
    formatter.format(value, this)
