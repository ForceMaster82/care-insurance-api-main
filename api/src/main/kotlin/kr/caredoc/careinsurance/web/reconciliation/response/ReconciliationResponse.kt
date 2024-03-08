package kr.caredoc.careinsurance.web.reconciliation.response

import kr.caredoc.careinsurance.reconciliation.ClosingStatus
import kr.caredoc.careinsurance.reconciliation.IssuedType

data class ReconciliationResponse(
    val id: String,
    val closingStatus: ClosingStatus,
    val receptionId: String,
    val caregivingRoundId: String,
    val billingAmount: Int,
    val settlementAmount: Int,
    val settlementDepositAmount: Int,
    val settlementWithdrawalAmount: Int,
    val issuedType: IssuedType,
    val profit: Int,
    val distributedProfit: Int,
)
