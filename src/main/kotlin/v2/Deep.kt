package deep.v2

public sealed class Deep<T>

public class DeepMap<T>(public val map: MutableMap<String, Deep<T>>) : Deep<T>() {
    public constructor(vararg entries: Pair<String, Deep<T>>) : this(mutableMapOf(*entries))

    public operator fun get(key: String): Deep<T>? = map[key]
    public operator fun set(key: String, value: Deep<T>) {
        map[key] = value
    }

    override fun toString(): String = map.toString()
}

public class DeepList<T>(public val list: MutableList<Deep<T>>) : Deep<T>() {
    public constructor(vararg elements: Deep<T>) : this(mutableListOf(*elements))

    public operator fun get(index: Int): Deep<T> = list[index]
    public operator fun set(index: Int, value: Deep<T>) {
        list[index] = value
    }

    public operator fun plusAssign(value: Deep<T>) {
        list += value
    }

    override fun toString(): String = list.toString()
}

public class DeepValue<T>(public var value: T) : Deep<T>() {
    override fun toString(): String = value.toString()
}

public operator fun <T> Deep<T>?.get(key: String): Deep<T>? = (this as? DeepMap<T>)?.get(key)
public operator fun <T> Deep<T>?.get(index: Int): Deep<T>? = (this as? DeepList<T>)?.get(index)

public val <T> Deep<T>?.map: Map<String, Deep<T>>? get() = (this as? DeepMap<T>)?.map
public val <T> Deep<T>?.list: List<Deep<T>>? get() = (this as? DeepList<T>)?.list
public val <T> Deep<T>?.value: T? get() = (this as? DeepValue<T>)?.value

public fun <T> deep(vararg entries: Pair<String, Deep<T>>): DeepMap<T> = DeepMap(*entries)
public fun <T> deep(vararg elements: Deep<T>): DeepList<T> = DeepList(*elements)
public fun <T> deep(value: T): DeepValue<T> = DeepValue(value)
