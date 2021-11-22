package deep

import serial.*
import java.io.Reader

public fun <T> Reader.parseDeep(handler: DeepEvent.Handler<T>, parser: ValueParser<T>): Unit = parse {
    skipWhitespace()
    parseDeep(handler, parser)
}

public fun <T> Reader.parseDeep(parser: ValueParser<T>): Deep<T> {
    val handler = DefaultHandler<T>()
    parseDeep(handler, parser)
    return handler.value ?: error("Expected end of input")
}

public fun interface ValueParser<out T> {
    public fun TextParseState.parseValue(): T
}

public sealed class DeepEvent<out T> {
    public fun interface Handler<T> {
        public fun handle(event: DeepEvent<T>)
    }

    // Events

    public object MapStart : DeepEvent<Nothing>() {
        override fun toString(): String = "MapStart"
    }

    public object MapEnd : DeepEvent<Nothing>() {
        override fun toString(): String = "MapEnd"
    }

    public object ListStart : DeepEvent<Nothing>() {
        override fun toString(): String = "ListStart"
    }

    public object ListEnd : DeepEvent<Nothing>() {
        override fun toString(): String = "ListEnd"
    }

    public class Key(public val key: String) : DeepEvent<Nothing>() {
        override fun toString(): String = "Key($key)"
        override fun hashCode(): Int = key.hashCode()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Key
            return key == other.key
        }
    }

    public class Value<T>(public val value: T) : DeepEvent<T>() {
        override fun toString(): String = "Value($value)"
        override fun hashCode(): Int = value.hashCode()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Value<*>
            return value == other.value
        }
    }
}

// Helpers

private fun <T> TextParseState.parseDeep(handler: DeepEvent.Handler<T>, parser: ValueParser<T>): Unit = when (current) {
    '{' -> parseMap(handler, parser)
    '[' -> parseList(handler, parser)
    else -> handler.handle(DeepEvent.Value(with(parser) { parseValue() }))
}

private fun <T> TextParseState.parseMap(handler: DeepEvent.Handler<T>, parser: ValueParser<T>) {
    handler.handle(DeepEvent.MapStart)
    parseCollection('}') {
        val key = decodeStringLiteral()
        handler.handle(DeepEvent.Key(key))
        skipWhitespace()
        if (!readOptionalChar(':')) readRequiredChar('=')
        skipWhitespace()
        parseDeep(handler, parser)
    }
    handler.handle(DeepEvent.MapEnd)
}

private fun <T> TextParseState.parseList(handler: DeepEvent.Handler<T>, parser: ValueParser<T>) {
    handler.handle(DeepEvent.ListStart)
    parseCollection(']') {
        parseDeep(handler, parser)
    }
    handler.handle(DeepEvent.ListEnd)
}

public fun TextParseState.decodeStringLiteral(): String {
    val delimiter = current
    ensure(delimiter == '"' || delimiter == '\'') { "Expected: \" or '" }
    next()
    startCapture()
    while (current != delimiter) {
        ensure(current >= '\u0020') { "Invalid character" }
        if (current == '\\') {
            pauseCapture()
            next()
            addToCapture(
                when (current) {
                    'n' -> '\n'
                    'r' -> '\r'
                    't' -> '\t'
                    'b' -> '\b'
                    'f' -> '\u000c'
                    'u' -> String(CharArray(4) {
                        next()
                        ensure(current.isHexDigit()) { "Invalid hex digit" }
                        current
                    }).toInt(16).toChar()
                    else -> current
                }
            )
            next()
            startCapture()
        } else {
            next()
        }
    }
    val string = finishCapture()
    next()
    return string
}

public fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

private inline fun TextParseState.parseCollection(end: Char, action: () -> Unit) {
    next()
    do {
        skipWhitespace()
        if (readOptionalChar(end)) return
        action()
        skipWhitespace()
    } while (readOptionalChar(','))
    readRequiredChar(end)
}

private class DefaultHandler<T> : DeepEvent.Handler<T> {
    private var handler: DeepCollectionHandler<T>? = null
    var value: Deep<T>? = null
        private set

    override fun handle(event: DeepEvent<T>) {
        check(value == null) { "Expected end of input" }
        when (event) {
            DeepEvent.MapStart -> handler = DeepMapHandler(handler)
            DeepEvent.MapEnd -> emit((handler as DeepMapHandler).unwrap())
            DeepEvent.ListStart -> handler = DeepListHandler(handler)
            DeepEvent.ListEnd -> emit((handler as DeepListHandler).unwrap())
            is DeepEvent.Key -> (handler as DeepMapHandler).accept(event.key)
            is DeepEvent.Value -> emit(handler, DeepValue(event.value))
        }
    }

    private fun emit(data: Pair<DeepCollectionHandler<T>?, Deep<T>>) = emit(data.first, data.second)
    private fun emit(parent: DeepCollectionHandler<T>?, value: Deep<T>) {
        if (parent == null) this.value = value else parent.accept(value)
        handler = parent
    }

    private sealed class DeepCollectionHandler<T>(private val parent: DeepCollectionHandler<T>?) {
        init {
            require(parent == null || parent.canWrap)
        }

        abstract val canWrap: Boolean
        abstract fun accept(deep: Deep<T>)
        protected abstract fun finish(): Deep<T>

        fun unwrap() = parent to finish()
    }

    private class DeepMapHandler<T>(parent: DeepCollectionHandler<T>?) : DeepCollectionHandler<T>(parent) {
        private var map: MutableMap<String, Deep<T>>? = mutableMapOf()
        private var key: String? = null
        override val canWrap get() = map != null && key != null

        fun accept(key: String) {
            check(map != null) { "Map already ended" }
            require(this.key == null) { "Expected value" }
            this.key = key
        }

        override fun accept(deep: Deep<T>) {
            (map ?: error("Map already ended"))[this.key ?: error("Expected key")] = deep
            key = null
        }

        override fun finish() = DeepMap(map ?: error("Map already ended")).also { map = null }
    }

    private class DeepListHandler<T>(parent: DeepCollectionHandler<T>?) : DeepCollectionHandler<T>(parent) {
        private var list: MutableList<Deep<T>>? = mutableListOf()
        override val canWrap get() = list != null

        override fun accept(deep: Deep<T>) {
            (list ?: error("List already ended")) += deep
        }

        override fun finish() = DeepList(list ?: error("List already ended")).also { list = null }
    }
}
