package deep

import kotlin.test.Test
import kotlin.test.assertEquals

class TestSerialization {
    @Test
    fun testSerialize() {
        assertEquals("5", writeDeepInt(value(5)))
        assertEquals("[5,6]", writeDeepInt(list(value(5), value(6))))
        assertEquals(
            """{"five":5,"rem":[6,404]}""",
            writeDeepInt(map(
                "five" to value(5),
                "rem" to list(value(6), value(404)),
            )),
        )
        assertEquals("""{"\u0001":5}""", writeDeepInt(map("\u0001" to value(5))))

        assertEquals("5", writeDeepIntNullable(value(5)))
        assertEquals("null", writeDeepIntNullable(value(null)))
        assertEquals("[5,6,null]", writeDeepIntNullable(list(value(5), value(6), value(null))))
    }

    @Test
    fun testEvents() {
        tokenizeInt("5") shouldBe listOf(DeepEvent.Value(5))
        tokenizeInt("[5, 6]") shouldBe listOf(
            DeepEvent.ListStart,
            DeepEvent.Value(5),
            DeepEvent.Value(6),
            DeepEvent.ListEnd,
        )
        tokenizeInt("[   5   ,   6   ,   ]") shouldBe listOf(
            DeepEvent.ListStart,
            DeepEvent.Value(5),
            DeepEvent.Value(6),
            DeepEvent.ListEnd,
        )
        tokenizeInt("""{"five": 5, "rem": [6, 404]}""") shouldBe listOf(
            DeepEvent.MapStart,
            DeepEvent.Key("five"),
            DeepEvent.Value(5),
            DeepEvent.Key("rem"),
            DeepEvent.ListStart,
            DeepEvent.Value(6),
            DeepEvent.Value(404),
            DeepEvent.ListEnd,
            DeepEvent.MapEnd,
        )
        tokenizeInt("""{"five": 5, "rem": [6, 404,],}""") shouldBe listOf(
            DeepEvent.MapStart,
            DeepEvent.Key("five"),
            DeepEvent.Value(5),
            DeepEvent.Key("rem"),
            DeepEvent.ListStart,
            DeepEvent.Value(6),
            DeepEvent.Value(404),
            DeepEvent.ListEnd,
            DeepEvent.MapEnd,
        )
    }

    @Test
    fun testDeserialize() {
        parseDeepInt("5") shouldBe 5
        parseDeepInt("[5, 6]") shouldBe listOf(5, 6)
        parseDeepInt("[   5   ,   6   ,   ]") shouldBe listOf(5, 6)
        parseDeepInt("""{"five": 5, "rem": [6, 404]}""") shouldBe mapOf("five" to 5, "rem" to listOf(6, 404))
        parseDeepInt("""{"five": 5, "rem": [6, 404,],}""") shouldBe mapOf("five" to 5, "rem" to listOf(6, 404))
        parseDeepInt("""{"\u0001": 5}""") shouldBe mapOf("\u0001" to 5)
        parseDeepInt("""{"\u0041": 5}""") shouldBe mapOf("A" to 5)

        parseDeepIntNullable("5") shouldBe 5
        parseDeepIntNullable("null") shouldBe null
        parseDeepIntNullable("[5, 6, null]") shouldBe listOf(5, 6, null)
    }
}
