package kr.caredoc.careinsurance.web.reception.response

import kr.caredoc.careinsurance.reception.modification.CaregivingChargeModificationHistory
import java.time.OffsetDateTime

class CaregivingChargeModificationHistoryResponse(
    val caregivingRoundNumber: Int,
    val modifiedProperty: CaregivingChargeModificationHistory.ModifiedProperty,
    val previous: Any?,
    val modified: Any?,
    val modifierId: String,
    val modifiedDateTime: OffsetDateTime,
)
