package kr.caredoc.careinsurance.patch

class EmptyPatch<T> : Patch<T> {
    override fun compareWith(originValue: T): Comparison<T> {
        return OriginOnlyComparison(originValue)
    }

    override val patchingValue: T?
        get() = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
