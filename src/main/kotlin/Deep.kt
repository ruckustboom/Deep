package deep

public sealed class Deep<out T> {
    public abstract fun <R> map(convert: (T) -> Deep<R>): Deep<R>
}

public class DeepMap<out T>(public val data: Map<String, Deep<T>>) : Deep<T>() {
    public constructor(vararg entries: Pair<String, Deep<T>>) : this(entries.toMap())

    override fun <R> map(convert: (T) -> Deep<R>): Deep<R> = DeepMap(data.mapValues { (_, it) -> it.map(convert) })
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

    override fun <R> map(convert: (T) -> Deep<R>): Deep<R> = DeepList(data.map { it.map(convert) })
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
    override fun <R> map(convert: (T) -> Deep<R>): Deep<R> = convert(data)
    override fun toString(): String = data.toString()
    override fun hashCode(): Int = data?.hashCode() ?: 0
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DeepValue<*>
        return data == other.data
    }
}

public fun <T> Deep<Deep<T>>.flatten(): Deep<T> = map { it }
