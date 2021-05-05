package deep

public sealed class Deep<out T> {
    public abstract fun <R> map(convert: (T) -> Deep<R>): Deep<R>
    public abstract fun <U, R> merge(b: Deep<U>, action: (T, Deep<U>) -> Deep<R>): Deep<R>
}

public class DeepMap<out T>(public val data: Map<String, Deep<T>>) : Deep<T>() {
    public constructor(vararg entries: Pair<String, Deep<T>>) : this(mapOf(*entries))

    override fun <R> map(convert: (T) -> Deep<R>): Deep<R> = DeepMap(data.mapValues { (_, it) -> it.map(convert) })
    override fun <U, R> merge(b: Deep<U>, action: (T, Deep<U>) -> Deep<R>): Deep<R> {
        b as DeepMap<U>
        return DeepMap(data.mapValues { (key, value) -> value.merge(b.data.getValue(key), action) })
    }

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
    public constructor(vararg elements: Deep<T>) : this(listOf(*elements))

    override fun <R> map(convert: (T) -> Deep<R>): Deep<R> = DeepList(data.map { it.map(convert) })
    override fun <U, R> merge(b: Deep<U>, action: (T, Deep<U>) -> Deep<R>): Deep<R> {
        b as DeepList<U>
        return DeepList(data.mapIndexed { index, value -> value.merge(b.data[index], action) })
    }

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
    override fun <U, R> merge(b: Deep<U>, action: (T, Deep<U>) -> Deep<R>): Deep<R> = action(data, b)

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
