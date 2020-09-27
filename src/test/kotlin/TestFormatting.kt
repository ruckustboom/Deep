package deep

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestFormatting {
    @Test
    fun test() {
        val value = DeepMap(
            "null" to null,
            "string" to DeepString("fred"),
            "empty-list" to DeepList(),
            "empty-map" to DeepMap(),
            "list" to DeepList(
                DeepString("alice"),
                DeepString("bob"),
                DeepString("charlie"),
            ),
            "map" to DeepMap(
                "a" to DeepString("alice"),
                "b" to DeepString("bob"),
                "c" to DeepString("charlie"),
            ),
            "map-of-list" to DeepMap(
                "list" to DeepList(
                    DeepString("one"),
                    DeepString("two"),
                )
            ),
            "list-of-map" to DeepList(
                DeepMap(
                    "one" to DeepString("1"),
                    "two" to DeepString("2",)
                )
            ),
        )
        assertEquals(
            """
            {
              "null": /,
              "string": "fred",
              "empty-list": [],
              "empty-map": {},
              "list": [
                "alice",
                "bob",
                "charlie"
              ],
              "map": {
                "a": "alice",
                "b": "bob",
                "c": "charlie"
              },
              "map-of-list": {"list": [
                "one",
                "two"
              ]},
              "list-of-map": [{
                "one": "1",
                "two": "2"
              }]
            }
            """.trimIndent().ignoreNewline(),
            DeepFormatter.SPACES(value).ignoreNewline()
        )
        assertEquals(
            """{"null":/,"string":"fred",""" +
                    """"empty-list":[],"empty-map":{},""" +
                    """"list":["alice","bob","charlie"],""" +
                    """"map":{"a":"alice","b":"bob","c":"charlie"},""" +
                    """"map-of-list":{"list":["one","two"]},""" +
                    """"list-of-map":[{"one":"1","two":"2"}]}""",
            DeepFormatter.MINIFIED(value)
        )
    }
}
