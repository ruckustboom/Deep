package deep

import java.io.Reader

public class DeepStringParser : Parser<DeepString<*>?>() {
    override fun parse(): DeepString<*>? {
        skipWhitespace()
        val result = readValue()
        skipWhitespace()
        return result
    }

    private fun readValue() = when (currentChar) {
        '"', '\'' -> DeepStringValue(readString())
        '[' -> readList()
        '{' -> readMap()
        '!', '-', '/', '*', 'X', 'x', '~' -> {
            read()
            null
        }
        else -> crash("Invalid character")
    }

    private fun readList(): DeepStringList {
        val list = mutableListOf<DeepString<*>?>()
        readCollection(']') {
            list += readValue()
        }
        return DeepStringList(list)
    }

    private fun readMap(): DeepStringMap {
        val map = mutableMapOf<String, DeepString<*>?>()
        readCollection('}') {
            val key = readString()
            skipWhitespace()
            readRequiredChar(':')
            skipWhitespace()
            map[key] = readValue()
        }
        return DeepStringMap(map)
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

        public fun parse(string: String): DeepString<*>? = DeepStringParser().parse(string)
        public fun parse(reader: Reader, bufferSize: Int = DEFAULT_BUFFER_SIZE): DeepString<*>? =
            DeepStringParser().parse(reader, bufferSize)

        public operator fun invoke(string: String): DeepString<*>? = parse(string)
        public operator fun invoke(reader: Reader, bufferSize: Int = DEFAULT_BUFFER_SIZE): DeepString<*>? =
            parse(reader, bufferSize)

        private fun Char.isHexDigit() = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
    }
}
