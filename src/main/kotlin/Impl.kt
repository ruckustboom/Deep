package deep

import serial.TextParseState
import serial.readLiteral
import java.io.Writer

// String

public object StringParser : ValueParser<String> {
    override fun TextParseState.parseValue(): String = decodeStringLiteral()
}

public object StringSerializer : ValueSerializer<String> {
    override fun Writer.serializeValue(value: String): Unit = encodeStringLiteral(value)
}

// Nullable

public class NullableParser<T : Any>(
    private val parser: ValueParser<T>,
    private val nullValue: String = "null",
) : ValueParser<T?> {
    init {
        require(nullValue.isNotEmpty())
    }

    override fun TextParseState.parseValue(): T? {
        return if (char == nullValue[0]) {
            readLiteral(nullValue)
            null
        } else with(parser) { parseValue() }
    }
}

public class NullableSerializer<T : Any>(
    private val serializer: ValueSerializer<T>,
    private val nullValue: String = "null",
) : ValueSerializer<T?> {
    init {
        require(nullValue.isNotEmpty())
    }

    override fun Writer.serializeValue(value: T?) {
        if (value == null) write(nullValue)
        else with(serializer) { serializeValue(value) }
    }
}
