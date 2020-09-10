package deep

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TestConstruction {
    @Test
    fun testDeepFunction() {
        Assertions.assertEquals(DeepStringValue("Fred"), deep("Fred"))
        Assertions.assertEquals(
            DeepStringList(listOf(DeepStringValue("Fred"), DeepStringValue("true"))),
            deep("Fred", true),
        )
        Assertions.assertEquals(
            DeepStringMap(
                mapOf(
                    "fred" to DeepStringValue("Fred"),
                    "bool" to DeepStringValue("true"),
                )
            ),
            deep("fred" to "Fred", "bool" to true),
        )
    }

    @Test
    fun testGetters() {
        val list = deep(
            null,
            true,
            5,
            6.5,
            "Fred",
            emptyList<Nothing>(),
            emptyMap<Nothing, Nothing>(),
        )
        list[0] shouldBe null
        list[1] shouldBe "true"
        list[2] shouldBe "5"
        list[3] shouldBe "6.5"
        list[4] shouldBe "Fred"
        list[5] shouldBe emptyList<Nothing>()
        list[6] shouldBe emptyMap<Nothing, Nothing>()

        val map = deep(
            "null" to null,
            "bool" to true,
            "int" to 5,
            "float" to 6.5,
            "string" to "Fred",
            "list" to emptyList<Nothing>(),
            "map" to emptyMap<Nothing, Nothing>(),
        )
        map["null"] shouldBe null
        map["bool"] shouldBe "true"
        map["int"] shouldBe "5"
        map["float"] shouldBe "6.5"
        map["string"] shouldBe "Fred"
        map["list"] shouldBe emptyList<Nothing>()
        map["map"] shouldBe emptyMap<Nothing, Nothing>()
        map["other"] shouldBe null
    }
}
