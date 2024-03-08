package kr.caredoc.careinsurance.patch

interface Comparison<T> {
    fun hasDifference(): Boolean

    fun ifHavingDifference(block: (T, T) -> Unit)

    val valueToOverwrite: T
}
