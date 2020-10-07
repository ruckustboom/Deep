package deep

import java.io.StringWriter
import java.io.Writer

public fun interface ValueSerializer<in T> {
    public fun Writer.serializeValue(value: T)
}

public class DeepSerializer<T> private constructor(
    private val serializer: ValueSerializer<T>,
    private val intent: String?,
) {
    public fun serialize(value: Deep<T>, writer: Writer) {
        writer.serializeDeep(value, 0)
    }

    private fun Writer.serializeDeep(value: Deep<T>, level: Int) {
        when (value) {
            is DeepMap -> serializeMap(value.data, level)
            is DeepList -> serializeList(value.data, level)
            is DeepValue -> with(serializer) { serializeValue(value.data) }
        }
    }

    private fun Writer.serializeMap(map: Map<String, Deep<T>>, level: Int) {
        append('{')
        when (map.size) {
            0 -> Unit
            1 -> serializeEntry(map.entries.first(), level)
            else -> {
                var index = 0
                for (entry in map) {
                    if (index++ > 0) append(',')
                    if (intent != null) appendLine()
                    addIndent(level + 1)
                    serializeEntry(entry, level + 1)
                }
                if (intent != null) appendLine()
                addIndent(level)
            }
        }
        append('}')
    }

    private fun Writer.serializeEntry(entry: Map.Entry<String, Deep<T>>, level: Int) {
        encodeStringLiteral(entry.key)
        append(':')
        if (intent != null) append(' ')
        serializeDeep(entry.value, level)
    }

    private fun Writer.serializeList(list: List<Deep<T>>, level: Int) {
        append('[')
        when (list.size) {
            0 -> Unit
            1 -> serializeDeep(list[0], level)
            else -> {
                list.forEachIndexed { i, v ->
                    if (i > 0) append(',')
                    if (list.size > 1) {
                        if (intent != null) appendLine()
                        addIndent(level + 1)
                        serializeDeep(v, level + 1)
                    }
                }
                if (intent != null) appendLine()
                addIndent(level)
            }
        }
        append(']')
    }

    private fun Writer.addIndent(level: Int) {
        if (intent != null) repeat(level) { write(intent) }
    }

    public companion object {
        public fun <T> tabs(serializer: ValueSerializer<T>): DeepSerializer<T> =
            DeepSerializer(serializer, "\t")

        public fun <T> spaces(count: Int = 2, serializer: ValueSerializer<T>): DeepSerializer<T> =
            spaces(serializer, count)

        public fun <T> spaces(serializer: ValueSerializer<T>, count: Int = 2): DeepSerializer<T> =
            DeepSerializer(serializer, " ".repeat(count))

        public fun <T> minified(serializer: ValueSerializer<T>): DeepSerializer<T> =
            DeepSerializer(serializer, null)
    }
}

public inline fun <T> DeepSerializer<T>.toString(value: Deep<T>): String =
    StringWriter().apply { use { serialize(value, it) } }.toString()

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
                val int = char.toInt()
                append(HEX_CHARS[int shr 4 and 0xf])
                append(HEX_CHARS[int and 0xf])
            }
            else -> append(char)
        }
    }
    append('"')
}
