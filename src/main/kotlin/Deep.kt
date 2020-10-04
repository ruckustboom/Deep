package deep

public sealed class Deep<out T>

public class DeepMap<out T>(public val map: Map<String, Deep<T>>) : Deep<T>() {
    public constructor(vararg entries: Pair<String, Deep<T>>) : this(mapOf(*entries))

    override fun toString(): String = map.toString()
    override fun hashCode(): Int = map.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DeepMap<*>
        return map == other.map
    }
}

public class DeepList<out T>(public val list: List<Deep<T>>) : Deep<T>() {
    public constructor(vararg elements: Deep<T>) : this(listOf(*elements))

    override fun toString(): String = list.toString()
    override fun hashCode(): Int = list.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DeepList<*>
        return list == other.list
    }
}

public class DeepValue<out T>(public val value: T) : Deep<T>() {
    override fun toString(): String = value.toString()
    override fun hashCode(): Int = value?.hashCode() ?: 0
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DeepValue<*>
        return value == other.value
    }
}
