package kr.caredoc.careinsurance.web.billing.response

import kr.caredoc.careinsurance.billing.BillingProgressingStatus
import java.time.LocalDate
import java.time.OffsetDateTime

data class BillingResponse(
    val id: String,
    val receptionId: String,
    val accidentNumber: String,
    val patientName: String,
    val roundNumber: Int,
    val startDateTime: OffsetDateTime,
    val endDateTime: OffsetDateTime,
    val actualUsagePeriod: String,
    val billingDate: LocalDate?,
    val totalAmount: Int,
    val totalDepositAmount: Int,
    val totalWithdrawalAmount: Int,
    val transactionDate: LocalDate?,
    val billingProgressingStatus: BillingProgressingStatus,
)
