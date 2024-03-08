package kr.caredoc.careinsurance.patch

interface Patch<T> {
    fun compareWith(originValue: T): Comparison<T>

    val patchingValue: T?

    fun expectValue(errorMessage: String): T = patchingValue ?: throw ExpectedPatchingValueNotExists(errorMessage)
}
