package kr.caredoc.careinsurance.security.accesscontrol

interface Action {
    operator fun get(attribute: ActionAttribute): Set<String> = setOf()
}
