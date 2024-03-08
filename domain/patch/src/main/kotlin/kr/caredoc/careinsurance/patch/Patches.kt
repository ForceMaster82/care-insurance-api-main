package kr.caredoc.careinsurance.patch

object Patches {
    fun <T> ofValue(value: T) = OverwritePatch(value)

    fun <T> ofEmpty() = EmptyPatch<T>()
}
