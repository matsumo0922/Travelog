package me.matsumo.travelog.core.common

inline fun <T> MutableCollection<T>.removeIf(predicate: (T) -> Boolean): Boolean {
    var removed = false
    val iterator = iterator()

    while (iterator.hasNext()) {
        if (predicate(iterator.next())) {
            iterator.remove()
            removed = true
        }
    }

    return removed
}
