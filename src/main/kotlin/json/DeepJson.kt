package deep.json

import deep.*
import java.io.Writer

public typealias DeepJson = Deep<JsonValue?>

public sealed class JsonValue
public class JsonString(public val value: String) : JsonValue()
public class JsonNumber(public val value: Number) : JsonValue()
public object JsonTrue : JsonValue()
public object JsonFalse : JsonValue()

public object JsonValueEncoder : ValueEncoder<JsonValue?> {
    override fun Writer.encodeValue(value: JsonValue?) {
        when (value) {
            null -> write("null")
            JsonTrue -> write("true")
            JsonFalse -> write("false")
            is JsonString -> encode(value.value)
            is JsonNumber -> write(value.value.toString())
        }
    }
}

public fun json(string: String): DeepJson = DeepValue(JsonString(string))
public fun json(number: Number): DeepJson = DeepValue(JsonNumber(number))
public fun json(boolean: Boolean): DeepJson = DeepValue(if (boolean) JsonTrue else JsonFalse)
public fun json(nothing: Nothing?): DeepJson = DeepValue(nothing)
public fun json(vararg entries: Pair<String, DeepJson>): DeepJson = DeepMap(*entries)
public fun json(vararg elements: DeepJson): DeepJson = DeepList(*elements)

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
    val formatter = DeepEncoder.minified(JsonValueEncoder)
    println(formatter.toString(json))
}
