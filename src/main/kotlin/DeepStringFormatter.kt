package deep

public class DeepStringFormatter private constructor(private val indentation: String?) {
    public operator fun invoke(value: DeepString<*>?): String = buildString { appendValue(value, 0) }

    private fun StringBuilder.appendValue(value: DeepString<*>?, level: Int) {
        when (value) {
            null -> append('/')
            is DeepStringValue -> encode(value.value)
            is DeepStringList -> appendList(value, level)
            is DeepStringMap -> appendMap(value, level)
        }
    }

    private fun StringBuilder.appendList(value: DeepStringList, level: Int) {
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

    private fun StringBuilder.appendMap(value: DeepStringMap, level: Int) {
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

    private fun StringBuilder.appendEntry(entry: Map.Entry<String, DeepString<*>?>, level: Int) {
        encode(entry.key)
        append(':')
        if (indentation != null) append(' ')
        appendValue(entry.value, level)
    }

    private fun StringBuilder.appendIndent(level: Int) {
        if (indentation == null) return
        repeat(level) { append(indentation) }
    }

    public companion object {
        public val TABS: DeepStringFormatter = DeepStringFormatter("\t")
        public val MINIFIED: DeepStringFormatter = DeepStringFormatter(null)
        public val SPACES: DeepStringFormatter = DeepStringFormatter("  ")

        private const val HEX_CHARS = "0123456789abcdef"
        private fun StringBuilder.encode(string: String) {
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
