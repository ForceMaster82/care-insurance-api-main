package kr.caredoc.careinsurance.security.accesscontrol

import kr.caredoc.careinsurance.security.authentication.CredentialExpiredException

object DenyExpiredCredentialPolicy : AccessPolicy {
    override fun check(sub: Subject, act: Action, obj: Object) {
        if (sub[SubjectAttribute.CREDENTIAL_EXPIRED].contains("true")) {
            throw CredentialExpiredException("만료된 크레덴셜로는 수행할 수 없는 행위입니다.")
        }
    }
}
