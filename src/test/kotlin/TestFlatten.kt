package deep

import kotlin.test.Test
import kotlin.test.assertIs

class TestFlatten {
    @Test
    fun test() {
        val d = map("a" to value(list(value(1), value(2))))
        assertIs<DeepValue<*>>(d.data["a"])
        val f = d.flatten()
        assertIs<DeepMap<*>>(f)
        assertIs<DeepList<*>>(f.data["a"])
        f shouldBe mapOf("a" to listOf(1, 2))
    }
}
