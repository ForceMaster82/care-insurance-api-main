package kr.caredoc.careinsurance.security.accesscontrol

fun AccessPolicy.checkAll(sub: Subject, act: Action, objs: Collection<Object>) = objs.forEach {
    this.check(sub, act, it)
}
