package deep

import java.io.Reader
import java.io.StringReader
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

public const val EOI: Char = '\u0000'

public abstract class Parser<T> {
    protected var currentChar: Char = EOI
        private set

    private lateinit var stream: Reader
    private var lineCount = 0
    private var lineStart = 0

    private var buffer: CharArray = CharArray(0)
    private var bufferStart = 0
    private var bufferLength = 0
    private var bufferHead = 0

    private val capture = StringBuilder()
    private var captureStart = -1

    private val lock = ReentrantLock()

    protected abstract fun parse(): T

    public fun parse(string: String): T = invoke(StringReader(string), string.length.coerceIn(1, DEFAULT_BUFFER_SIZE))
    public fun parse(reader: Reader, bufferSize: Int = DEFAULT_BUFFER_SIZE): T = lock.withLock {
        require(bufferSize > 0) { "Buffer size must be > 0" }
        reader.use {
            currentChar = EOI
            stream = it
            lineCount = 0
            lineStart = 0
            buffer = CharArray(bufferSize)
            bufferStart = 0
            bufferLength = 0
            bufferHead = 0
            captureStart = -1
            // Parse
            read()
            val result = parse()
            ensure(currentChar == EOI) { "Expected EOI" }
            result
        }
    }

    public operator fun invoke(string: String): T = parse(string)
    public operator fun invoke(reader: Reader, bufferSize: Int = DEFAULT_BUFFER_SIZE): T = parse(reader, bufferSize)

    // Implementation

    /**
     * Read in the next character fro the stream
     *
     * Any function that reads data from the stream should ultimately lead here.
     * This function automatically handles buffering the data and keeping track
     * of the position in the file.
     */
    protected fun read() {
        // Update buffer if depleted
        if (bufferHead == bufferLength) {
            // Dump capture if currently capturing
            if (captureStart != -1) {
                capture.append(buffer, captureStart, bufferLength - captureStart)
                captureStart = 0
            }
            bufferStart += bufferLength
            bufferLength = stream.read(buffer, 0, buffer.size)
            bufferHead = 0
            if (bufferLength == -1) {
                currentChar = EOI
                bufferHead++
                return
            }
        }
        // Check for newline
        if (currentChar == '\n') {
            lineCount++
            lineStart = bufferStart + bufferHead
        }
        currentChar = buffer[bufferHead++]
    }

    protected fun startCapture() {
        captureStart = bufferHead - 1
    }

    protected fun pauseCapture() {
        val end = if (currentChar == EOI) bufferHead else bufferHead - 1
        capture.append(buffer, captureStart, end - captureStart)
        captureStart = -1
    }

    protected fun capture(char: Char) {
        capture.append(char)
    }

    protected fun finishCapture(): String {
        val start = captureStart
        val end = bufferHead - 1
        captureStart = -1
        return if (capture.isNotEmpty()) {
            capture.append(buffer, start, end - start)
            val captured = capture.toString()
            capture.setLength(0)
            captured
        } else String(buffer, start, end - start)
    }

    // Helpers

    protected fun getLocation(): Location {
        val offset = bufferStart + bufferHead - 1
        val column = offset - lineStart
        return Location(offset, lineCount, column)
    }

    protected inline fun readWhile(predicate: (Char) -> Boolean) {
        while (predicate(currentChar)) read()
    }

    protected inline fun readIf(predicate: (Char) -> Boolean): Boolean = if (predicate(currentChar)) {
        read()
        true
    } else false

    // Exception handling

    protected inline fun ensure(condition: Boolean, message: () -> String) {
        if (!condition) crash(message())
    }

    protected fun crash(message: String, cause: Throwable? = null): Nothing =
        throw ParseException(getLocation(), currentChar, message, cause)
}

public class ParseException(
    public val location: Location,
    public val character: Char,
    public val description: String,
    cause: Throwable? = null
) : Exception("$description (found <$character>/${character.toInt()} at $location)", cause)

public data class Location(val offset: Int, val line: Int, val column: Int)
