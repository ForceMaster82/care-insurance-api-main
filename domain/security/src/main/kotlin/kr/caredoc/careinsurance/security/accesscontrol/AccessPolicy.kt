package kr.caredoc.careinsurance.security.accesscontrol

interface AccessPolicy {
    fun check(sub: Subject, act: Action, obj: Object)
}
