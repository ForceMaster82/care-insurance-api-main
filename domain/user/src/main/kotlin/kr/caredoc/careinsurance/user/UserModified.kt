package kr.caredoc.careinsurance.user

import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import java.time.LocalDateTime

data class UserModified(
    val userId: String,
    val suspended: Modification<Boolean>,
    val editSubject: Subject,
) {

    val modifiedDateTime: LocalDateTime = Clock.now()
}
