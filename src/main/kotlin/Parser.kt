package deep

import java.io.Reader

/**
 * This method does **NOT** close the reader or check for input exhaustion. It
 * is up to the user to handle that.
 */
public inline fun <T> Reader.parse(parse: ParseState.() -> T): T = initParse().parse()
public fun Reader.initParse(): ParseState = ParseStateImpl(this).apply { next() }

public interface ParseState {
    public val offset: Int
    public val lineCount: Int
    public val lineStart: Int
    public val char: Char
    public val isEndOfInput: Boolean
    public fun next()
    public fun startCapture()
    public fun addToCapture(char: Char)
    public fun pauseCapture()
    public fun finishCapture(): String
}

private class ParseStateImpl(private val stream: Reader) : ParseState {
    override var offset = -1
    override var lineCount = 0
    override var lineStart = 0
    override var char = '\u0000'
    override var isEndOfInput = true

    // Read

    override fun next() {
        // Check for newline
        if (isCapturing) capture.append(char)
        if (char == '\n') {
            lineCount++
            lineStart = offset
        }
        val next = stream.read()
        if (next >= 0) {
            offset++
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
        ensure(!isCapturing) { "Already capturing" }
        isCapturing = true
    }

    override fun pauseCapture() {
        ensure(isCapturing) { "Not currently capturing" }
        isCapturing = false
    }

    override fun addToCapture(char: Char) {
        capture.append(char)
    }

    override fun finishCapture(): String {
        ensure(isCapturing) { "Not currently capturing" }
        isCapturing = false
        val string = capture.toString()
        capture.setLength(0)
        return string
    }
}

public class ParseException(
    public val location: Location,
    public val character: Char,
    public val description: String,
    cause: Throwable? = null,
) : Exception("$description (found <$character>/${character.toInt()} at $location)", cause)

public data class Location(val offset: Int, val line: Int, val lineOffset: Int)

// Some common helpers

public inline fun ParseState.readWhile(predicate: (Char) -> Boolean) {
    while (!isEndOfInput && predicate(char)) next()
}

public inline fun ParseState.readIf(predicate: (Char) -> Boolean): Boolean = if (!isEndOfInput && predicate(char)) {
    next()
    true
} else false

public inline fun ParseState.ensure(condition: Boolean, message: () -> String) {
    if (!condition) crash(message())
}

public fun ParseState.crash(message: String, cause: Throwable? = null): Nothing =
    throw ParseException(getLocation(), char, message, cause)

public fun ParseState.getLocation(): Location {
    val lineOffset = offset - lineStart
    return Location(offset, lineCount, lineOffset)
}
