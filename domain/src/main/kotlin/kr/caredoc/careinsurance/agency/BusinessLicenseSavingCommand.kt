package kr.caredoc.careinsurance.agency

import kr.caredoc.careinsurance.security.accesscontrol.Action
import kr.caredoc.careinsurance.security.accesscontrol.ActionAttribute
import kr.caredoc.careinsurance.security.accesscontrol.ActionType
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import java.io.InputStream

data class BusinessLicenseSavingCommand(
    val externalCaregivingOrganizationId: String,
    val businessLicenseFile: InputStream,
    val businessLicenseFileName: String,
    val mime: String,
    val contentLength: Long,
    val subject: Subject,
) : Action {
    override fun get(attribute: ActionAttribute) = when (attribute) {
        ActionAttribute.ACTION_TYPE -> setOf(ActionType.CREATE)
        else -> setOf()
    }
}
