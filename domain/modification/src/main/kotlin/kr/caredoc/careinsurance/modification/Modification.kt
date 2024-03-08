package kr.caredoc.careinsurance.modification

import kr.caredoc.careinsurance.then

data class Modification<T>(
    val previous: T,
    val current: T,
) {
    fun <U> map(mapper: (T) -> U): Modification<U> {
        return Modification(
            mapper(previous),
            mapper(current),
        )
    }

    val hasChanged = previous != current

    fun ifChanged(block: Modification<T>.() -> Unit): IfChanged {
        hasChanged.then { block(this) }

        return IfChanged()
    }

    inner class IfChanged {
        fun orElse(block: Modification<T>.() -> Unit) {
            (!hasChanged).then { block(this@Modification) }
        }
    }
}
