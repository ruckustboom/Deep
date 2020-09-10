package deep

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestFormatting {
    @Test
    fun test() {
        val value = deep(
            "null" to null,
            "boolean" to true,
            "integer" to 5,
            "decimal" to -Double.NaN,
            "string" to "fred",
            "empty-list" to emptyList<Nothing>(),
            "empty-map" to emptyMap<Nothing, Nothing>(),
            "single-list" to listOf(7),
            "single-map" to mapOf("complex" to false),
            "multi-list" to listOf("alice", "bob", "charlie"),
            "multi-map" to mapOf("alice" to 27.0, "bob" to 43.5, "charlie" to 35.9),
            "map-of-list" to mapOf("list" to listOf("one", "two")),
            "list-of-map" to listOf(mapOf("one" to 1, "two" to 2)),
        )
        assertEquals(
            """
            {
              "null": /,
              "boolean": "true",
              "integer": "5",
              "decimal": "NaN",
              "string": "fred",
              "empty-list": [],
              "empty-map": {},
              "single-list": ["7"],
              "single-map": {"complex": "false"},
              "multi-list": [
                "alice",
                "bob",
                "charlie"
              ],
              "multi-map": {
                "alice": "27.0",
                "bob": "43.5",
                "charlie": "35.9"
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
            DeepStringFormatter.SPACES(value).ignoreNewline()
        )
        assertEquals(
            """{"null":/,"boolean":"true","integer":"5","decimal":"NaN","string":"fred",""" +
                    """"empty-list":[],"empty-map":{},""" +
                    """"single-list":["7"],"single-map":{"complex":"false"},""" +
                    """"multi-list":["alice","bob","charlie"],""" +
                    """"multi-map":{"alice":"27.0","bob":"43.5","charlie":"35.9"},""" +
                    """"map-of-list":{"list":["one","two"]},""" +
                    """"list-of-map":[{"one":"1","two":"2"}]}""",
            DeepStringFormatter.MINIFIED(value)
        )
    }
}
