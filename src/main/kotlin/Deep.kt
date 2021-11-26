package deep

public sealed class Deep<out T>

public class DeepMap<out T>(public val data: Map<String, Deep<T>>) : Deep<T>() {
    public constructor(vararg entries: Pair<String, Deep<T>>) : this(entries.toMap())

    override fun toString(): String = data.toString()
    override fun hashCode(): Int = data.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DeepMap<*>
        return data == other.data
    }
}

public class DeepList<out T>(public val data: List<Deep<T>>) : Deep<T>() {
    public constructor(vararg elements: Deep<T>) : this(elements.toList())

    override fun toString(): String = data.toString()
    override fun hashCode(): Int = data.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DeepList<*>
        return data == other.data
    }
}

public class DeepValue<out T>(public val data: T) : Deep<T>() {
    override fun toString(): String = data.toString()
    override fun hashCode(): Int = data?.hashCode() ?: 0
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DeepValue<*>
        return data == other.data
    }
}

// Common Helpers

public fun <T, R> Deep<T>.map(convert: (T) -> Deep<R>): Deep<R> = when(this) {
    is DeepMap -> DeepMap(data.mapValues { (_, it) -> it.map(convert) })
    is DeepList -> DeepList(data.map { it.map(convert) })
    is DeepValue -> convert(data)
}

public fun <T> Deep<Deep<T>>.flatten(): Deep<T> = map { it }
