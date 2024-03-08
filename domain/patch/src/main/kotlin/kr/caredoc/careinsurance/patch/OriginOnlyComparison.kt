package kr.caredoc.careinsurance.patch

class OriginOnlyComparison<T>(
    val originValue: T
) : Comparison<T> {
    override fun hasDifference() = false

    override fun ifHavingDifference(block: (T, T) -> Unit) {
        return
    }

    override val valueToOverwrite: T
        get() = originValue
}
