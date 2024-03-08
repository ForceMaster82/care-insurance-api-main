package kr.caredoc.careinsurance.reception

import kr.caredoc.careinsurance.security.accesscontrol.Action
import kr.caredoc.careinsurance.security.accesscontrol.ActionAttribute
import kr.caredoc.careinsurance.security.accesscontrol.ActionType
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import java.io.InputStream

data class ReceptionApplicationCreationCommand(
    val receptionId: String,
    val fileName: String,
    val file: InputStream,
    val contentLength: Long,
    val mime: String,
    val subject: Subject
) : Action {
    override fun get(attribute: ActionAttribute) = when (attribute) {
        ActionAttribute.ACTION_TYPE -> setOf(ActionType.CREATE)
        else -> setOf()
    }
}
