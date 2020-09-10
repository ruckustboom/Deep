package deep

import org.junit.jupiter.api.Test

class TestParse {
    @Test
    fun testNull() {
        "/" shouldBe null
    }

    @Test
    fun testString() {
        "\"\"" shouldBe ""
        "\"null\"" shouldBe "null"
        "\"fred\"" shouldBe "fred"
        "'fred'" shouldBe "fred"
        "'\"'" shouldBe "\""
        "\"'\"" shouldBe "'"
        "\" \\n\\f\\t\\\\\\'\\/\\u0010\"" shouldBe " \n\u000c\t\\'/\u0010"
    }

    @Test
    fun testList() {
        "[]" shouldBe listOf<Nothing>()
        "[/]" shouldBe listOf(null)
        """["fred"]""" shouldBe listOf("fred")
        """["false", "17", /, "-6.2", "He said \"Hi!\""]""" shouldBe listOf("false", "17", null, "-6.2", "He said \"Hi!\"")
    }

    @Test
    fun testMap() {
        "{}" shouldBe mapOf<String, Nothing>()
        "{\"null\": /}" shouldBe mapOf("null" to null)
        "{\"boolean\": \"false\"}" shouldBe mapOf("boolean" to "false")
        """{"n": /, "b": "true", "i": "7", "d": "0.9", "s": "bob"}""" shouldBe mapOf(
            "n" to null,
            "b" to "true",
            "i" to "7",
            "d" to "0.9",
            "s" to "bob"
        )
        "{\"\\u0010\\\"\":\"\\u0011\"}" shouldBe mapOf("\u0010\"" to "\u0011")
    }

    @Test
    fun testComplex() {
        """
        {
            "fist-name": "Ruckus",
            "last-name": "T-Boom",
            "sex": "M",
            "born": {
                "year": "1990",
                "month": "January",
                "day": "2"
            },
            "married": "true",
            "employment": [
                {
                    "title": "Developer",
                    "employer": "i5 Services"
                },
                {
                    "title": "Software Engineer Intern",
                    "employer": "Family Search"
                },
                /,
                {
                    "title": "Internet Applications Developer",
                    "employer": "Xactware"
                },
                {
                    "title": "Bored",
                    "employer": /
                }
            ]
        }
        """ shouldBe mapOf(
            "fist-name" to "Ruckus",
            "last-name" to "T-Boom",
            "sex" to "M",
            "born" to mapOf(
                "year" to "1990",
                "month" to "January",
                "day" to "2",
            ),
            "married" to "true",
            "employment" to listOf(
                mapOf(
                    "title" to "Developer",
                    "employer" to "i5 Services",
                ),
                mapOf(
                    "title" to "Software Engineer Intern",
                    "employer" to "Family Search",
                ),
                null,
                mapOf(
                    "title" to "Internet Applications Developer",
                    "employer" to "Xactware",
                ),
                mapOf(
                    "title" to "Bored",
                    "employer" to null,
                ),
            ),
        )
    }

    @Test
    fun testFail() {
        "".shouldFail(0, "Invalid character", '\u0000')
        "a".shouldFail(0, "Invalid character", 'a')
        "5".shouldFail(0, "Invalid character", '5')
        "fred".shouldFail(0, "Invalid character", 'f')
        "[0]".shouldFail(1, "Invalid character", '0')
        "{ 5 }".shouldFail(2, "Expected: \" or '", '5')
        "\"string".shouldFail(7, "Invalid character", '\u0000')
        "\"fred'".shouldFail(6, "Invalid character", '\u0000')
        "'fred\"".shouldFail(6, "Invalid character", '\u0000')
        "\"\\u001g\"".shouldFail(6, "Invalid hex digit", 'g')
        "{ \"fred\" }".shouldFail(9, "Expected: :", '}')
        "[ \"fred\": 7 ]".shouldFail(8, "Expected: ]", ':')

        """
        {
          "first": "true",
          "also-first": "false",
          "number"
        }
        """.trimIndent().shouldFail(57, 4, 0, "Expected: :", '}')
    }
}
