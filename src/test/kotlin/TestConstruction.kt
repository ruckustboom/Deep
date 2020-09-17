package deep

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TestConstruction {
    @Test
    fun testDeepFunction() {
        Assertions.assertEquals(DeepString("Fred"), deep("Fred"))
        Assertions.assertEquals(
            DeepList(listOf(DeepString("Fred"), DeepString("true"))),
            deep("Fred", true),
        )
        Assertions.assertEquals(
            DeepMap(
                mapOf(
                    "fred" to DeepString("Fred"),
                    "bool" to DeepString("true"),
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

    @Test
    fun testAccessors() {
        val n = deep(null)
        val string = deep("a")
        val list = deep("a", "b")
        val map = deep("a" to "a")

        assertNull(n.string)
        assertNull(n.list)
        assertNull(n.map)

        assertNotNull(string.string)
        assertNull(string.list)
        assertNull(string.map)

        assertNull(list.string)
        assertNotNull(list.list)
        assertNull(list.map)

        assertNull(map.string)
        assertNull(map.list)
        assertNotNull(map.map)
    }
}
