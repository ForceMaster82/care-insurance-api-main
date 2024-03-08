package kr.caredoc.careinsurance.web.reception.response

import kr.caredoc.careinsurance.reception.history.ReceptionModificationHistory
import java.time.OffsetDateTime

data class ReceptionModificationHistoryResponse(
    val modifiedProperty: ReceptionModificationHistory.ModificationProperty,
    val previous: Any?,
    val modified: Any?,
    val modifierId: String,
    val modifiedDateTime: OffsetDateTime,
)
