package deep

import kotlin.test.Test
import kotlin.test.assertEquals

class TestSerialization {
    @Test
    fun testSerialize() {
        assertEquals("5", serialize(value(5)))
        assertEquals("[5,6]", serialize(list(value(5), value(6))))
        assertEquals(
            """{"five":5,"rem":[6,404]}""",
            serialize(
                map(
                    "five" to value(5),
                    "rem" to list(value(6), value(404)),
                )
            ),
        )
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
        deserialize("5") shouldBe 5
        deserialize("[5, 6]") shouldBe listOf(5, 6)
        deserialize("[   5   ,   6   ,   ]") shouldBe listOf(5, 6)
        deserialize("""{"five": 5, "rem": [6, 404]}""") shouldBe mapOf("five" to 5, "rem" to listOf(6, 404))
        deserialize("""{"five": 5, "rem": [6, 404,],}""") shouldBe mapOf("five" to 5, "rem" to listOf(6, 404))
    }
}
