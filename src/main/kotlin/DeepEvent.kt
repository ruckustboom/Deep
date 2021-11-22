package deep

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

public class DefaultHandler<T> : DeepEvent.Handler<T> {
    private var handler: DeepCollectionHandler<T>? = null
    public var value: Deep<T>? = null
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
