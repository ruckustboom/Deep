package deep

import kotlin.test.Test
import kotlin.test.assertEquals

class TestSerialization {
    @Test
    fun testSerialize() {
        assertEquals("5", writeDeep(value(5)))
        assertEquals("[5,6]", writeDeep(list(value(5), value(6))))
        assertEquals(
            """{"five":5,"rem":[6,404]}""",
            writeDeep(map(
                "five" to value(5),
                "rem" to list(value(6), value(404)),
            )),
        )
        assertEquals("""{"\u0001":5}""", writeDeep(map("\u0001" to value(5))))
    }

    @Test
    fun testEvents() {
        tokenize("5") shouldBe listOf(DeepEvent.Value(5))
        tokenize("[5, 6]") shouldBe listOf(
            DeepEvent.ListStart,
            DeepEvent.Value(5),
            DeepEvent.Value(6),
            DeepEvent.ListEnd,
        )
        tokenize("[   5   ,   6   ,   ]") shouldBe listOf(
            DeepEvent.ListStart,
            DeepEvent.Value(5),
            DeepEvent.Value(6),
            DeepEvent.ListEnd,
        )
        tokenize("""{"five": 5, "rem": [6, 404]}""") shouldBe listOf(
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
        tokenize("""{"five": 5, "rem": [6, 404,],}""") shouldBe listOf(
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
        parseDeep("5") shouldBe 5
        parseDeep("[5, 6]") shouldBe listOf(5, 6)
        parseDeep("[   5   ,   6   ,   ]") shouldBe listOf(5, 6)
        parseDeep("""{"five": 5, "rem": [6, 404]}""") shouldBe mapOf("five" to 5, "rem" to listOf(6, 404))
        parseDeep("""{"five": 5, "rem": [6, 404,],}""") shouldBe mapOf("five" to 5, "rem" to listOf(6, 404))
        parseDeep("""{"\u0001": 5}""") shouldBe mapOf("\u0001" to 5)
        parseDeep("""{"\u0041": 5}""") shouldBe mapOf("A" to 5)
    }
}
