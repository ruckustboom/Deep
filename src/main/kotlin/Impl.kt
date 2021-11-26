package deep

import serial.*
import java.io.InputStream
import java.io.OutputStream
import java.io.Writer

// String

public object StringReader : ValueReader<String> {
    override fun TextParseState.readValue(): String = decodeStringLiteral()
}

public object StringWriter : ValueWriter<String> {
    override fun Writer.writeValue(value: String): Unit = encodeStringLiteral(value)
}

public object StringParser : ValueParser<String> {
    override fun InputStream.parseValue(): String = readString()
}

public object StringSerializer : ValueSerializer<String> {
    override fun OutputStream.serializeValue(value: String): Unit = writeString(value)
}

// Nullable

public class NullableReader<T : Any>(
    private val reader: ValueReader<T>,
    private val nullValue: String = "null",
) : ValueReader<T?> {
    init {
        require(nullValue.isNotEmpty())
    }

    override fun TextParseState.readValue(): T? = if (current == nullValue[0]) {
        readLiteral(nullValue)
        null
    } else with(reader) { readValue() }
}

public class NullableWriter<T : Any>(
    private val writer: ValueWriter<T>,
    private val nullValue: String = "null",
) : ValueWriter<T?> {
    init {
        require(nullValue.isNotEmpty())
    }

    override fun Writer.writeValue(value: T?) {
        if (value == null) write(nullValue)
        else with(writer) { writeValue(value) }
    }
}

public class NullableParser<T : Any>(private val parser: ValueParser<T>) : ValueParser<T?> {
    override fun InputStream.parseValue(): T? = readNullable { with(parser) { parseValue() } }
}

public class NullableSerializer<T : Any>(private val serializer: ValueSerializer<T>) : ValueSerializer<T?> {
    override fun OutputStream.serializeValue(value: T?): Unit =
        writeNullable(value) { with(serializer) { serializeValue(it) } }
}
