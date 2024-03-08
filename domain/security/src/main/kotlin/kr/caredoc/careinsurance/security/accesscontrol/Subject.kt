package kr.caredoc.careinsurance.security.accesscontrol

interface Subject {
    companion object {
        fun fromAttributes(vararg attributes: Pair<SubjectAttribute, Set<String>>): Subject {
            val attributeMap = mapOf(*attributes)

            return object : Subject {
                override fun get(attribute: SubjectAttribute): Set<String> {
                    return attributeMap[attribute] ?: setOf()
                }
            }
        }
    }

    operator fun get(attribute: SubjectAttribute): Set<String> = setOf()
}
