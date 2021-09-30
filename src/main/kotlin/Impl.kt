package deep

import parse.TextParseState
import parse.readLiteral
import java.io.Writer

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
