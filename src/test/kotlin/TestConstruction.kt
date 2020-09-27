package deep

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TestConstruction {
    @Test
    fun testGetters() {
        val list = DeepList(
            null,
            DeepString("Fred"),
            DeepList(),
            DeepMap(),
        )
        list[0] shouldBe null
        list[1] shouldBe "Fred"
        list[2] shouldBe emptyList<Nothing>()
        list[3] shouldBe emptyMap<Nothing, Nothing>()

        val map = DeepMap(
            "null" to null,
            "string" to DeepString("Fred"),
            "list" to DeepList(),
            "map" to DeepMap(),
        )
        map["null"] shouldBe null
        map["string"] shouldBe "Fred"
        map["list"] shouldBe emptyList<Nothing>()
        map["map"] shouldBe emptyMap<Nothing, Nothing>()
        map["other"] shouldBe null
    }

    @Test
    fun testAccessors() {
        val n = null
        val string = DeepString("a")
        val list = DeepList(DeepString("a"), DeepString("b"))
        val map = DeepMap("a" to DeepString("a"))

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
