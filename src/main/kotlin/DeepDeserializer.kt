package deep

import parse.*
import java.io.Reader

public fun interface ValueDeserializer<out T> {
    public fun TextParseState.deserializeValue(): T
}

public fun <T> Reader.deserializeDeep(deserializer: ValueDeserializer<T>): Deep<T> = parse {
    skipWhitespace()
    return deserializeDeep(deserializer)
}

private fun <T> TextParseState.deserializeDeep(deserializer: ValueDeserializer<T>): Deep<T> = when (char) {
    '{' -> deserializeMap(deserializer)
    '[' -> deserializeList(deserializer)
    else -> DeepValue(with(deserializer) { deserializeValue() })
}

private fun <T> TextParseState.deserializeMap(deserializer: ValueDeserializer<T>): DeepMap<T> {
    val map = mutableMapOf<String, Deep<T>>()
    deserializeCollection('}') {
        val key = decodeStringLiteral()
        skipWhitespace()
        if (!readOptionalChar(':')) readRequiredChar('=')
        skipWhitespace()
        map[key] = deserializeDeep(deserializer)
    }
    return DeepMap(map)
}

private fun <T> TextParseState.deserializeList(deserializer: ValueDeserializer<T>): DeepList<T> {
    val list = mutableListOf<Deep<T>>()
    deserializeCollection(']') {
        list += deserializeDeep(deserializer)
    }
    return DeepList(list)
}

// TODO: Allow trailing commas
private inline fun TextParseState.deserializeCollection(end: Char, action: () -> Unit) {
    next()
    skipWhitespace()
    if (readOptionalChar(end)) return
    do {
        skipWhitespace()
        action()
        skipWhitespace()
    } while (readOptionalChar(','))
    readRequiredChar(end)
}
