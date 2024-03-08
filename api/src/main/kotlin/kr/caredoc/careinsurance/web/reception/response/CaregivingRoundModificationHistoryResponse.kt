package kr.caredoc.careinsurance.web.reception.response

import kr.caredoc.careinsurance.reception.modification.CaregivingRoundModificationHistory
import java.time.OffsetDateTime

class CaregivingRoundModificationHistoryResponse(
    val caregivingRoundNumber: Int,
    val modifiedProperty: CaregivingRoundModificationHistory.ModifiedProperty,
    val previous: Any?,
    val modified: Any?,
    val modifierId: String,
    val modifiedDateTime: OffsetDateTime,
)
