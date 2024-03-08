package kr.caredoc.careinsurance.patch

class OverwritePatch<T>(
    override val patchingValue: T,
) : Patch<T> {
    override fun compareWith(originValue: T): Comparison<T> {
        return ValueToValueComparison(
            originValue = originValue,
            patchingValue = patchingValue,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OverwritePatch<*>

        if (patchingValue != other.patchingValue) return false

        return true
    }

    override fun hashCode(): Int {
        return patchingValue?.hashCode() ?: 0
    }
}
