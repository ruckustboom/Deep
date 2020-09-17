package deep

import java.io.Reader

public class DeepParser : Parser<Deep<*>?>() {
    override fun parse(): Deep<*>? {
        skipWhitespace()
        val result = readValue()
        skipWhitespace()
        return result
    }

    private fun readValue() = when (currentChar) {
        '"', '\'' -> DeepString(readString())
        '[' -> readList()
        '{' -> readMap()
        '!', '-', '/', '*', 'X', 'x', '~' -> {
            read()
            null
        }
        else -> crash("Invalid character")
    }

    private fun readList(): DeepList {
        val list = mutableListOf<Deep<*>?>()
        readCollection(']') {
            list += readValue()
        }
        return DeepList(list)
    }

    private fun readMap(): DeepMap {
        val map = mutableMapOf<String, Deep<*>?>()
        readCollection('}') {
            val key = readString()
            skipWhitespace()
            readRequiredChar(':')
            skipWhitespace()
            map[key] = readValue()
        }
        return DeepMap(map)
    }

    private fun readString(): String {
        val delim = currentChar
        ensure(delim == '"' || delim == '\'') { "Expected: \" or '" }
        readRequiredChar(delim)
        startCapture()
        while (currentChar != delim) {
            ensure(currentChar >= '\u0020') { "Invalid character" }
            if (currentChar == '\\') {
                pauseCapture()
                read()
                capture(
                    when (currentChar) {
                        'n' -> '\n'
                        'r' -> '\r'
                        't' -> '\t'
                        'b' -> '\b'
                        'f' -> '\u000c'
                        'u' -> String(CharArray(4) {
                            read()
                            ensure(currentChar.isHexDigit()) { "Invalid hex digit" }
                            currentChar
                        }).toInt(16).toChar()
                        else -> currentChar
                    }
                )
                read()
                startCapture()
            } else {
                read()
            }
        }
        val string = finishCapture()
        read()
        return string
    }

    private fun readRequiredChar(char: Char) = ensure(readOptionalChar(char)) { "Expected: $char" }
    private fun readOptionalChar(char: Char) = readIf { it.toLowerCase() == char.toLowerCase() }

    private inline fun readCollection(end: Char, action: () -> Unit) {
        read()
        skipWhitespace()
        if (readOptionalChar(end)) return
        do {
            skipWhitespace()
            action()
            skipWhitespace()
        } while (readOptionalChar(','))
        readRequiredChar(end)
    }

    private fun skipWhitespace() = readWhile { it in WHITESPACE }

    public companion object {
        private const val WHITESPACE = " \t\n\r"

        public fun parse(string: String): Deep<*>? = DeepParser().parse(string)
        public fun parse(reader: Reader, bufferSize: Int = DEFAULT_BUFFER_SIZE): Deep<*>? =
            DeepParser().parse(reader, bufferSize)

        public operator fun invoke(string: String): Deep<*>? = parse(string)
        public operator fun invoke(reader: Reader, bufferSize: Int = DEFAULT_BUFFER_SIZE): Deep<*>? =
            parse(reader, bufferSize)

        private fun Char.isHexDigit() = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
    }
}
