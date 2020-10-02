package deep.v2

import java.io.Writer

public typealias DeepJson = Deep<JsonValue?>

public sealed class JsonValue
public class JsonString(public val value: String) : JsonValue()
public class JsonNumber(public val value: Number) : JsonValue()
public object JsonTrue : JsonValue()
public object JsonFalse : JsonValue()

public object JsonValueWriter : ValueWriter<JsonValue?> {
    override fun Writer.append(value: JsonValue?) {
        when (value) {
            null -> write("null")
            JsonTrue -> write("true")
            JsonFalse -> write("false")
            is JsonString -> encode(value.value)
            is JsonNumber -> write(value.value.toString())
        }
    }
}

public fun json(string: String): DeepJson = deep(JsonString(string))
public fun json(number: Number): DeepJson = deep(JsonNumber(number))
public fun json(boolean: Boolean): DeepJson = deep(if (boolean) JsonTrue else JsonFalse)
public fun json(nothing: Nothing?): DeepJson = deep(nothing)
public fun json(vararg entries: Pair<String, DeepJson>): DeepJson = deep(*entries)
public fun json(vararg elements: DeepJson): DeepJson = deep(*elements)

public fun main() {
    val json = json(
        "str" to json("Fred"),
        "num" to json(99.5),
        "true" to json(true),
        "false" to json(false),
        "null" to json(null),
        "list" to json(
            json("Fred"),
            json(99.5),
            json(true),
            json(false),
            json(null),
        )
    )
    val formatter = DeepFormatter.minified(JsonValueWriter)
    println(formatter.toString(json))
}
