package deep

import java.io.StringWriter
import java.io.Writer

public fun interface ValueEncoder<T> {
    public fun Writer.encodeValue(value: T)
}

public class DeepEncoder<T> private constructor(
    private val valueEncoder: ValueEncoder<T>,
    private val indentation: String?,
) {
    public fun encode(value: Deep<T>, writer: Writer) {
        writer.encodeDeep(value, 0)
    }

    private fun Writer.encodeDeep(value: Deep<T>, level: Int) {
        when (value) {
            is DeepMap -> encodeMap(value.map, level)
            is DeepList -> encodeList(value.list, level)
            is DeepValue -> with(valueEncoder) { encodeValue(value.value) }
        }
    }

    private fun Writer.encodeMap(map: Map<String, Deep<T>>, level: Int) {
        append('{')
        when (map.size) {
            0 -> Unit
            1 -> encodeEntry(map.entries.first(), level)
            else -> {
                var index = 0
                for (entry in map) {
                    if (index++ > 0) append(',')
                    if (indentation != null) appendLine()
                    encodeIndent(level + 1)
                    encodeEntry(entry, level + 1)
                }
                if (indentation != null) appendLine()
                encodeIndent(level)
            }
        }
        append('}')
    }

    private fun Writer.encodeEntry(entry: Map.Entry<String, Deep<T>>, level: Int) {
        encode(entry.key)
        append(':')
        if (indentation != null) append(' ')
        encodeDeep(entry.value, level)
    }

    private fun Writer.encodeList(list: List<Deep<T>>, level: Int) {
        append('[')
        when (list.size) {
            0 -> Unit
            1 -> encodeDeep(list[0], level)
            else -> {
                list.forEachIndexed { i, v ->
                    if (i > 0) append(',')
                    if (list.size > 1) {
                        if (indentation != null) appendLine()
                        encodeIndent(level + 1)
                        encodeDeep(v, level + 1)
                    }
                }
                if (indentation != null) appendLine()
                encodeIndent(level)
            }
        }
        append(']')
    }

    private fun Writer.encodeIndent(level: Int) {
        if (indentation != null) repeat(level) { write(indentation) }
    }

    public companion object {
        public fun <T> tabs(valueEncoder: ValueEncoder<T>): DeepEncoder<T> = DeepEncoder(valueEncoder, "\t")
        public fun <T> spaces(count: Int = 2, encoder: ValueEncoder<T>): DeepEncoder<T> = spaces(encoder, count)
        public fun <T> spaces(valueEncoder: ValueEncoder<T>, count: Int = 2): DeepEncoder<T> =
            DeepEncoder(valueEncoder, " ".repeat(count))

        public fun <T> minified(valueEncoder: ValueEncoder<T>): DeepEncoder<T> = DeepEncoder(valueEncoder, null)
    }
}

public inline fun <T> DeepEncoder<T>.toString(value: Deep<T>): String =
    StringWriter().apply { use { encode(value, it) } }.toString()

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
