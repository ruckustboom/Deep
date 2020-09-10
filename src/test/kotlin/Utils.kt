package deep

import org.junit.jupiter.api.Assertions.*

infix fun String.shouldBe(expected: Any?) = DeepStringParser(this) shouldBe expected
infix fun DeepString<*>?.shouldBe(expected: Any?) {
    when (this) {
        null -> assertNull(expected)
        is DeepStringValue -> {
            assertTrue(expected is String)
            assertEquals(expected as String, value)
        }
        is DeepStringList -> {
            assertTrue(expected is List<*>)
            expected as List<*>
            assertEquals(expected.size, value.size)
            expected.forEachIndexed { index, innerExpected ->
                value[index] shouldBe innerExpected
            }
        }
        is DeepStringMap -> {
            assertTrue(expected is Map<*, *>)
            expected as Map<*, *>
            assertEquals(expected.keys, value.keys)
            for ((expectedKey, expectedValue) in expected) {
                assertTrue(expectedKey is String)
                expectedKey as String
                assertTrue(expectedKey in value)
                val innerValue = value[expectedKey]
                innerValue shouldBe expectedValue
            }
        }
    }
}

fun String.shouldFail(offset: Int, description: String, character: Char) =
    shouldFail(offset, 0, offset, description, character)

fun String.shouldFail(offset: Int, line: Int, column: Int, description: String, character: Char) = try {
    DeepStringParser(this)
    fail<Nothing>("Input parsed successfully")
} catch (e: ParseException) {
    assertEquals(offset, e.location.offset)
    assertEquals(line, e.location.line)
    assertEquals(column, e.location.column)
    assertEquals(description, e.description)
    assertEquals(character, e.character)
}

fun String.ignoreNewline() = replace("\r", "").replace("\n", "")
