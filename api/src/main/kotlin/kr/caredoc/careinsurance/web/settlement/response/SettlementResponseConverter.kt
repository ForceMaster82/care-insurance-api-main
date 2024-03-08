package kr.caredoc.careinsurance.web.settlement.response

import kr.caredoc.careinsurance.intoUtcOffsetDateTime
import kr.caredoc.careinsurance.reception.Reception
import kr.caredoc.careinsurance.reception.ReferenceReceptionNotExistsException
import kr.caredoc.careinsurance.settlement.Settlement
object SettlementResponseConverter {
    fun intoSettlementResponse(reception: Reception, settlement: Settlement) = SettlementResponse(
        id = settlement.id,
        receptionId = settlement.receptionId,
        caregivingRoundId = settlement.caregivingRoundId,
        accidentNumber = settlement.accidentNumber,
        caregivingRoundNumber = settlement.caregivingRoundNumber,
        progressingStatus = settlement.progressingStatus,
        patientName = reception.patientInfo.name.masked,
        dailyCaregivingCharge = settlement.dailyCaregivingCharge,
        basicAmount = settlement.basicAmount,
        additionalAmount = settlement.additionalAmount,
        totalAmount = settlement.totalAmount,
        lastCalculationDateTime = settlement.lastCalculationDateTime.intoUtcOffsetDateTime(),
        expectedSettlementDate = settlement.expectedSettlementDate,
        totalDepositAmount = settlement.totalDepositAmount,
        totalWithdrawalAmount = settlement.totalWithdrawalAmount,
        lastTransactionDateTime = settlement.lastTransactionDatetime?.intoUtcOffsetDateTime(),
        settlementCompletionDateTime = settlement.settlementCompletionDateTime?.intoUtcOffsetDateTime(),
        settlementManagerId = settlement.settlementManagerId,
    )

    fun intoSettlementResponses(receptions: Map<String, Reception>, settlements: Collection<Settlement>) =
        settlements.map {
            val reception = receptions[it.receptionId]
                ?: throw ReferenceReceptionNotExistsException(it.receptionId)
            intoSettlementResponse(reception, it)
        }
}
