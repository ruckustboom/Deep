package deep

import serial.TextParseState
import serial.readLiteral
import java.io.Writer

// String

public object StringReader : ValueReader<String> {
    override fun TextParseState.readValue(): String = decodeStringLiteral()
}

public object StringWriter : ValueWriter<String> {
    override fun Writer.writeValue(value: String): Unit = encodeStringLiteral(value)
}

// Nullable

public class NullableReader<T : Any>(
    private val parser: ValueReader<T>,
    private val nullValue: String = "null",
) : ValueReader<T?> {
    init {
        require(nullValue.isNotEmpty())
    }

    override fun TextParseState.readValue(): T? = if (current == nullValue[0]) {
        readLiteral(nullValue)
        null
    } else with(parser) { readValue() }
}

public class NullableWriter<T : Any>(
    private val serializer: ValueWriter<T>,
    private val nullValue: String = "null",
) : ValueWriter<T?> {
    init {
        require(nullValue.isNotEmpty())
    }

    override fun Writer.writeValue(value: T?) {
        if (value == null) write(nullValue)
        else with(serializer) { writeValue(value) }
    }
}
