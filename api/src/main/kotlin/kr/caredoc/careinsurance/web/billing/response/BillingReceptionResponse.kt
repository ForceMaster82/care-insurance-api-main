package kr.caredoc.careinsurance.web.billing.response

import kr.caredoc.careinsurance.billing.BillingProgressingStatus
import java.time.LocalDate
import java.time.OffsetDateTime

data class BillingReceptionResponse(
    val id: String,
    val caregivingRoundId: String,
    val caregivingRoundNumber: Int,
    val billingProgressingStatus: BillingProgressingStatus,
    val startDateTime: OffsetDateTime,
    val endDateTime: OffsetDateTime,
    val billingDate: LocalDate?,
    val basicAmount: Int,
    val additionalAmount: Int,
    val totalAmount: Int,
)
