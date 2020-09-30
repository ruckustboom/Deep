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
        public fun get(value: Deep<*>?, index: Int): Deep<*>? = value[index]

        @JvmStatic
        public fun get(value: Deep<*>?, key: String): Deep<*>? = value[key]

        @JvmStatic
        public fun containsKey(value: Deep<*>?, key: String): Boolean = key in value
    }
}

public class DeepMap(value: Map<String, Deep<*>?>) : Deep<Map<String, Deep<*>?>>(value) {
    public constructor(vararg entries: Pair<String, Deep<*>?>) : this(entries.toMap())
}

public class DeepList(value: List<Deep<*>?>) : Deep<List<Deep<*>?>>(value) {
    public constructor(vararg values: Deep<*>?) : this(values.toList())
}

public class DeepString(value: String) : Deep<String>(value)

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
