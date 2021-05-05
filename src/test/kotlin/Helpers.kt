package deep

import parse.TextParseState
import parse.captureWhile
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

object IntSerializer : ValueSerializer<Int> {
    override fun Writer.serializeValue(value: Int) = write(value.toString())
}

object IntDeserializer : ValueDeserializer<Int> {
    override fun TextParseState.deserializeValue() = captureWhile { it.isDigit() }.toInt()
}

fun serialize(deep: Deep<Int>): String = DeepSerializer.minified(IntSerializer).toString(deep)
fun deserialize(string: String): Deep<Int> = StringReader(string).deserializeDeep(IntDeserializer)
