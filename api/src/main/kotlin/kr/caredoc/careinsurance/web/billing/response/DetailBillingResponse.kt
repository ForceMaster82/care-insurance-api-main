package kr.caredoc.careinsurance.web.billing.response

import kr.caredoc.careinsurance.billing.Billing
import java.time.LocalDate
import java.time.OffsetDateTime

data class DetailBillingResponse(
    val accidentNumber: String,
    val subscriptionDate: LocalDate,
    val roundNumber: Int,
    val startDateTime: OffsetDateTime,
    val endDateTime: OffsetDateTime,
    val basicAmounts: List<Billing.BasicAmount>,
    val additionalHours: Int,
    val additionalAmount: Int,
    val totalAmount: Int,
    val receptionId: String,
    val totalDepositAmount: Int,
    val totalWithdrawalAmount: Int,
)
