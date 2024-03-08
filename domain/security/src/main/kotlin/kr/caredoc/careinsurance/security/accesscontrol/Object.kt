package kr.caredoc.careinsurance.security.accesscontrol

interface Object {
    operator fun get(attribute: ObjectAttribute): Set<String> = setOf()
    fun isEmpty() = false
    fun isNotEmpty() = !isEmpty()

    object Empty : Object {
        override fun isEmpty() = true
    }
}
