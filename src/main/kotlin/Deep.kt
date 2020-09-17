package deep

public sealed class Deep<S : Any>(public val value: S) {
    override fun toString(): String = value.toString()
    override fun hashCode(): Int = value.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Deep<*>
        return value == other.value
    }

    public companion object {
        @JvmStatic
        public fun deep(value: Any?): Deep<*>? = deep.deep(value)

        @JvmStatic
        public fun deep(vararg values: Any?): DeepList = deep.deep(*values)

        @JvmStatic
        public fun deep(vararg entries: Pair<*, *>): DeepMap = deep.deep(*entries)

        @JvmStatic
        public fun get(value: Deep<*>?, index: Int): Deep<*>? = value[index]

        @JvmStatic
        public fun get(value: Deep<*>?, key: String): Deep<*>? = value[key]

        @JvmStatic
        public fun containsKey(value: Deep<*>?, key: String): Boolean = key in value
    }
}

public class DeepMap(value: Map<String, Deep<*>?>) : Deep<Map<String, Deep<*>?>>(value)
public class DeepList(value: List<Deep<*>?>) : Deep<List<Deep<*>?>>(value)
public class DeepString(value: String) : Deep<String>(value)

@JvmSynthetic
public fun deep(value: Any?): Deep<*>? = when (value) {
    null -> null
    is Deep<*> -> value
    is Iterable<*> -> DeepList(value.map(::deep))
    is Array<*> -> DeepList(value.map(::deep))
    is BooleanArray -> DeepList(value.map(::deep))
    is ByteArray -> DeepList(value.map(::deep))
    is UByteArray -> DeepList(value.map(::deep))
    is CharArray -> DeepList(value.map(::deep))
    is ShortArray -> DeepList(value.map(::deep))
    is UShortArray -> DeepList(value.map(::deep))
    is IntArray -> DeepList(value.map(::deep))
    is UIntArray -> DeepList(value.map(::deep))
    is LongArray -> DeepList(value.map(::deep))
    is ULongArray -> DeepList(value.map(::deep))
    is FloatArray -> DeepList(value.map(::deep))
    is DoubleArray -> DeepList(value.map(::deep))
    is Map<*, *> -> DeepMap(value.entries.associate { (k, v) -> k.toString() to deep(v) })
    else -> DeepString(value.toString())
}

@JvmSynthetic
public fun deep(vararg entries: Pair<*, *>): DeepMap = DeepMap(entries.associate { (k, v) -> k.toString() to deep(v) })

@JvmSynthetic
public fun deep(vararg values: Any?): DeepList = DeepList(values.map(::deep))

@JvmSynthetic
public operator fun Deep<*>?.get(index: Int): Deep<*>? = list?.getOrNull(index)

@JvmSynthetic
public operator fun Deep<*>?.get(key: String): Deep<*>? = map?.get(key)

@JvmSynthetic
public operator fun Deep<*>?.contains(key: String): Boolean = this is DeepMap && key in value

public val Deep<*>?.string: String?
    @JvmSynthetic
    get() = (this as? DeepString)?.value

public val Deep<*>?.list: List<Deep<*>?>?
    @JvmSynthetic
    get() = (this as? DeepList)?.value

public val Deep<*>?.map: Map<String, Deep<*>?>?
    @JvmSynthetic
    get() = (this as? DeepMap)?.value
