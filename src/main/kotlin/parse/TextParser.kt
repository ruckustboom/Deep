package parse

import java.io.Reader
import java.io.StringReader

/**
 * This method does **NOT** close the reader or check for input exhaustion. It
 * is up to the user to handle that.
 */
public inline fun <T> Reader.parse(parse: TextParseState.() -> T): T = initParse().parse()
public fun Reader.initParse(): TextParseState = TextParseStateImpl(this).apply { next() }

public inline fun <T> String.parse(
    consumeAll: Boolean = true,
    parse: TextParseState.() -> T,
): T = StringReader(this).use {
    val state = it.initParse()
    val value = state.parse()
    if (consumeAll && state.offset < length) state.crash("Unexpected: ${state.char} (${state.offset} vs $length)")
    value
}

public interface TextParseState {
    public val offset: Int
    public val lineCount: Int
    public val lineStart: Int
    public val char: Char
    public val isEndOfInput: Boolean
    public fun next()
    public fun startCapture()
    public fun pauseCapture()
    public fun addToCapture(char: Char)
    public fun finishCapture(): String
}

private class TextParseStateImpl(private val stream: Reader) : TextParseState {
    override var offset = -1
    override var lineCount = 0
    override var lineStart = 0
    override var char = '\u0000'
    override var isEndOfInput = false

    // Read

    override fun next() {
        ensure(!isEndOfInput) { "Unexpected EOI" }
        if (isCapturing) addToCapture(char)
        // Check for newline
        if (char == '\n') {
            lineCount++
            lineStart = offset
        }
        val next = stream.read()
        offset++
        if (next >= 0) {
            char = next.toChar()
            isEndOfInput = false
        } else {
            char = '\u0000'
            isEndOfInput = true
        }
    }

    // Capture

    private val capture = StringBuilder()
    private var isCapturing = false

    override fun startCapture() {
        isCapturing = true
    }

    override fun pauseCapture() {
        isCapturing = false
    }

    override fun addToCapture(char: Char) {
        capture.append(char)
    }

    override fun finishCapture(): String {
        isCapturing = false
        val string = capture.toString()
        capture.setLength(0)
        return string
    }
}

public class TextParseException(
    public val offset: Int,
    public val line: Int,
    public val column: Int,
    public val character: Char,
    public val description: String,
    cause: Throwable? = null,
) : Exception("$description (found <$character>/${character.code} at $offset ($line:$column))", cause)

// Some common helpers

public fun TextParseState.crash(message: String, cause: Throwable? = null): Nothing =
    throw TextParseException(offset, lineCount, offset - lineStart, char, message, cause)

public inline fun TextParseState.ensure(condition: Boolean, message: () -> String) {
    if (!condition) crash(message())
}

public inline fun TextParseState.readIf(predicate: (Char) -> Boolean): Boolean =
    if (!isEndOfInput && predicate(char)) {
        next()
        true
    } else false

public inline fun TextParseState.readWhile(predicate: (Char) -> Boolean) {
    while (!isEndOfInput && predicate(char)) next()
}

public inline fun TextParseState.captureWhile(predicate: (Char) -> Boolean): String {
    startCapture()
    readWhile(predicate)
    return finishCapture()
}

public fun TextParseState.skipWhitespace(): Unit = readWhile { it.isWhitespace() }

public fun TextParseState.decodeStringLiteral(): String {
    val delimiter = char
    ensure(delimiter == '"' || delimiter == '\'') { "Expected: \" or '" }
    next()
    startCapture()
    while (char != delimiter) {
        ensure(char >= '\u0020') { "Invalid character" }
        if (char == '\\') {
            pauseCapture()
            next()
            addToCapture(
                when (char) {
                    'n' -> '\n'
                    'r' -> '\r'
                    't' -> '\t'
                    'b' -> '\b'
                    'f' -> '\u000c'
                    'u' -> String(CharArray(4) {
                        next()
                        ensure(char.isHexDigit()) { "Invalid hex digit" }
                        char
                    }).toInt(16).toChar()
                    else -> char
                }
            )
            next()
            startCapture()
        } else {
            next()
        }
    }
    val string = finishCapture()
    next()
    return string
}

public fun TextParseState.readRequiredChar(char: Char, ignoreCase: Boolean = false): Unit =
    ensure(readOptionalChar(char, ignoreCase)) { "Expected: $char" }

public fun TextParseState.readOptionalChar(char: Char, ignoreCase: Boolean = false): Boolean =
    readIf { it.equals(char, ignoreCase) }

public fun TextParseState.readLiteral(literal: String, ignoreCase: Boolean = false) {
    for (char in literal) readRequiredChar(char, ignoreCase)
}

public fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
