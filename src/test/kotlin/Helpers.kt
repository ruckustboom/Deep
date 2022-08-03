package deep

import serial.TextCursor
import serial.captureWhile
import java.io.StringReader
import java.io.Writer
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

private val writeInt: Writer.(Int) -> Unit = { write(it.toString()) }
private val readInt: TextCursor.() -> Int = { captureWhile { it.isDigit() }.toInt() }
fun writeDeep(deep: Deep<Int>): String = deep.toStringMinified(writeInt)
fun readDeep(string: String) = StringReader(string).readDeep(readInt)

fun tokenize(string: String): List<DeepEvent<Int>> {
    val tokenizer = Tokenizer<Int>()
    StringReader(string).readDeep(tokenizer, readInt)
    return tokenizer.tokens
}

class Tokenizer<T> : DeepEvent.Handler<T> {
    val tokens = mutableListOf<DeepEvent<T>>()

    override fun handle(event: DeepEvent<T>) {
        tokens += event
    }
}
