package deep

import deep.common.readNullable
import deep.common.writeNullable
import serial.CharCursor
import serial.captureWhile
import kotlin.test.assertEquals

fun <T> value(value: T) = DeepValue(value)
fun <T> list(vararg values: Deep<T>) = DeepList(*values)
fun <T> map(vararg values: Pair<String, Deep<T>>) = DeepMap(*values)

infix fun <T> Deep<T>.shouldBe(expected: Any?) {
    when (this) {
        is DeepMap -> {
            expected as Map<*, *>
            assertEquals(expected.keys, data.keys)
            data.forEach { (key, value) -> value shouldBe expected[key] }
        }

        is DeepList -> {
            expected as List<*>
            assertEquals(expected.size, data.size)
            data.forEachIndexed { index, value -> value shouldBe expected[index] }
        }

        is DeepValue -> assertEquals(expected, data)
    }
}

infix fun <T> List<T>.shouldBe(expected: List<T>) {
    assertEquals(expected, this)
}

private val writeInt: Appendable.(Int) -> Unit = { append(it.toString()) }
private val readInt: CharCursor.() -> Int = { captureWhile { it.isDigit() }.toInt() }
fun writeDeepInt(deep: Deep<Int>): String = deep.toStringMinified(writeInt)
fun parseDeepInt(string: String) = string.parseDeep(readInt)

fun writeDeepIntNullable(deep: Deep<Int?>): String = deep.toStringMinified {
    writeNullable(it, writeValue = writeInt)
}

fun parseDeepIntNullable(string: String) = string.parseDeep {
    readNullable(readValue = readInt)
}

fun tokenizeInt(string: String): List<DeepEvent<Int>> {
    val tokenizer = Tokenizer<Int>()
    string.parseDeep(tokenizer, readInt)
    return tokenizer.tokens
}

class Tokenizer<T> : DeepEvent.Handler<T> {
    val tokens = mutableListOf<DeepEvent<T>>()

    override fun handle(event: DeepEvent<T>) {
        tokens += event
    }
}
