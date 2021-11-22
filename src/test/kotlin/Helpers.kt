package deep

import serial.TextParseState
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

object IntSerializer : ValueSerializer<Int> {
    override fun Writer.serializeValue(value: Int) = write(value.toString())
}

object IntDeserializer : ValueParser<Int> {
    override fun TextParseState.parseValue() = captureWhile { it.isDigit() }.toInt()
}

fun serialize(deep: Deep<Int>): String = DeepSerializer.minified(IntSerializer).toString(deep)
fun deserialize(string: String): Deep<Int> = StringReader(string).parseDeep(IntDeserializer)

fun tokenize(string: String): List<DeepEvent<Int>> {
    val tokenizer = Tokenizer<Int>()
    StringReader(string).parseDeep(tokenizer, IntDeserializer)
    return tokenizer.tokens
}

class Tokenizer<T> : DeepEvent.Handler<T> {
    val tokens = mutableListOf<DeepEvent<T>>()

    override fun handle(event: DeepEvent<T>) {
        tokens += event
    }
}
