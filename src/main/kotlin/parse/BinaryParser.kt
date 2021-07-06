package parse

import java.io.*

public interface BinaryParseState {
    public val offset: Int
    public val byte: Byte
    public val isEndOfInput: Boolean
    public fun next()
    public fun startCapture()
    public fun pauseCapture()
    public fun addToCapture(byte: Byte)
    public fun finishCapture(): ByteArray
}

public class BinaryParseException(
    public val offset: Int,
    public val byte: Byte,
    public val description: String,
    cause: Throwable? = null,
) : Exception("$description (found $byte at $offset)", cause)

// Implementation

private class BinaryParseStateImpl(private val stream: InputStream) : BinaryParseState {
    override var offset = -1
    override var byte: Byte = 0
    override var isEndOfInput = false

    // Read

    override fun next() {
        ensure(!isEndOfInput) { "Unexpected EOI" }
        if (isCapturing) addToCapture(byte)
        val next = stream.read()
        offset++
        if (next >= 0) {
            byte = next.toByte()
            isEndOfInput = false
        } else {
            byte = 0
            isEndOfInput = true
        }
    }

    // Capture

    private var capture: ByteArrayOutputStream? = null
    private var isCapturing = false

    override fun startCapture() {
        isCapturing = true
    }

    override fun pauseCapture() {
        isCapturing = false
    }

    override fun addToCapture(byte: Byte) {
        capture?.write(byte.toInt())
    }

    override fun finishCapture(): ByteArray {
        isCapturing = false
        return capture?.let {
            it.close()
            capture = null
            it.toByteArray()
        } ?: ByteArray(0)
    }
}

public fun InputStream.initParse(): BinaryParseState = BinaryParseStateImpl(this).apply { next() }

// Some common helpers

/**
 * This method does **NOT** close the input stream or check for input
 * exhaustion. It is up to the user to handle that.
 */
public inline fun <T> InputStream.parse(parse: BinaryParseState.() -> T): T = initParse().parse()

public inline fun <T> ByteArray.parse(
    consumeAll: Boolean = true,
    parse: BinaryParseState.() -> T,
): T = ByteArrayInputStream(this).use {
    val state = it.initParse()
    val value = state.parse()
    if (consumeAll && state.offset < size) state.crash("Unexpected: ${state.byte} (${state.offset} vs $size)")
    value
}

public inline fun BinaryParseState.ensure(condition: Boolean, message: () -> String) {
    if (!condition) crash(message())
}

public fun BinaryParseState.crash(message: String, cause: Throwable? = null): Nothing =
    throw BinaryParseException(offset, byte, message, cause)

public inline fun BinaryParseState.readIf(predicate: (Byte) -> Boolean): Boolean =
    if (!isEndOfInput && predicate(byte)) {
        next()
        true
    } else false

public inline fun BinaryParseState.readWhile(predicate: (Byte) -> Boolean) {
    while (!isEndOfInput && predicate(byte)) next()
}

public inline fun BinaryParseState.captureWhile(predicate: (Byte) -> Boolean): ByteArray {
    startCapture()
    readWhile(predicate)
    return finishCapture()
}
