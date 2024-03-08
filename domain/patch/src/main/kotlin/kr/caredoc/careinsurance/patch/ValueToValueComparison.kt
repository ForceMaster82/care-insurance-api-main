package kr.caredoc.careinsurance.patch

class ValueToValueComparison<T>(
    val originValue: T,
    val patchingValue: T
) : Comparison<T> {
    override fun hasDifference() = originValue != patchingValue

    override fun ifHavingDifference(block: (T, T) -> Unit) {
        if (!hasDifference()) {
            return
        }

        block(originValue, patchingValue)
    }

    override val valueToOverwrite: T
        get() = if (hasDifference()) {
            patchingValue
        } else {
            originValue
        }
}
