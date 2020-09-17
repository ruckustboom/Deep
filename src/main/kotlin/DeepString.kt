package deep

public sealed class DeepString<S : Any>(public val value: S) {
    override fun toString(): String = value.toString()
    override fun hashCode(): Int = value.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DeepString<*>
        return value == other.value
    }

    public companion object {
        @JvmStatic
        public fun deep(value: Any?): DeepString<*>? = deep.deep(value)

        @JvmStatic
        public fun deep(vararg values: Any?): DeepStringList = deep.deep(*values)

        @JvmStatic
        public fun deep(vararg entries: Pair<*, *>): DeepStringMap = deep.deep(*entries)

        @JvmStatic
        public fun get(value: DeepString<*>?, index: Int): DeepString<*>? = value[index]

        @JvmStatic
        public fun get(value: DeepString<*>?, key: String): DeepString<*>? = value[key]

        @JvmStatic
        public fun containsKey(value: DeepString<*>?, key: String): Boolean = key in value
    }
}

public class DeepStringMap(value: Map<String, DeepString<*>?>) : DeepString<Map<String, DeepString<*>?>>(value)
public class DeepStringList(value: List<DeepString<*>?>) : DeepString<List<DeepString<*>?>>(value)
public class DeepStringValue(value: String) : DeepString<String>(value)

@JvmSynthetic
public fun deep(value: Any?): DeepString<*>? = when (value) {
    null -> null
    is DeepString<*> -> value
    is Collection<*> -> DeepStringList(value.map(::deep))
    is Array<*> -> DeepStringList(value.map(::deep))
    is BooleanArray -> DeepStringList(value.map(::deep))
    is ByteArray -> DeepStringList(value.map(::deep))
    is UByteArray -> DeepStringList(value.map(::deep))
    is CharArray -> DeepStringList(value.map(::deep))
    is ShortArray -> DeepStringList(value.map(::deep))
    is UShortArray -> DeepStringList(value.map(::deep))
    is IntArray -> DeepStringList(value.map(::deep))
    is UIntArray -> DeepStringList(value.map(::deep))
    is LongArray -> DeepStringList(value.map(::deep))
    is ULongArray -> DeepStringList(value.map(::deep))
    is FloatArray -> DeepStringList(value.map(::deep))
    is DoubleArray -> DeepStringList(value.map(::deep))
    is Map<*, *> -> DeepStringMap(value.entries.associate { (k, v) -> k.toString() to deep(v) })
    else -> DeepStringValue(value.toString())
}

@JvmSynthetic
public fun deep(vararg entries: Pair<*, *>): DeepStringMap =
    DeepStringMap(entries.associate { (k, v) -> k.toString() to deep(v) })

@JvmSynthetic
public fun deep(vararg values: Any?): DeepStringList = DeepStringList(values.map(::deep))

@JvmSynthetic
public operator fun DeepString<*>?.get(index: Int): DeepString<*>? = (this as? DeepStringList)?.value?.getOrNull(index)

@JvmSynthetic
public operator fun DeepString<*>?.get(key: String): DeepString<*>? = (this as? DeepStringMap)?.value?.get(key)

@JvmSynthetic
public operator fun DeepString<*>?.contains(key: String): Boolean = this is DeepStringMap && key in value

public val DeepString<*>?.string: String?
    @JvmSynthetic
    get() = (this as? DeepStringValue)?.value

public val DeepString<*>?.list: List<DeepString<*>?>?
    @JvmSynthetic
    get() = (this as? DeepStringList)?.value

public val DeepString<*>?.map: Map<String, DeepString<*>?>?
    @JvmSynthetic
    get() = (this as? DeepStringMap)?.value
