package kr.caredoc.careinsurance.reconciliation

import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganization
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationsByIdsQuery
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationsByIdsQueryHandler
import kr.caredoc.careinsurance.caregiving.CaregivingRound
import kr.caredoc.careinsurance.caregiving.CaregivingRoundsByIdsQuery
import kr.caredoc.careinsurance.caregiving.CaregivingRoundsByIdsQueryHandler
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import org.springframework.stereotype.Component
import java.util.StringJoiner

@Component
class ReconciliationCsvTemplate(
    private val caregivingRoundsByIdsQueryHandler: CaregivingRoundsByIdsQueryHandler,
    private val externalCaregivingOrganizationsByIdsQueryHandler: ExternalCaregivingOrganizationsByIdsQueryHandler,
) {
    companion object {
        private const val HEADER = "사고번호,고객명,간병차수,청구금액,간병인명,정산금액,출금액,입금액,회차수익,케어닥수익,제휴사,분배수익"
    }

    fun generate(data: Collection<Reconciliation>, subject: Subject): String {

        val joiner = StringJoiner("\n")
        joiner.add(HEADER)

        if (data.isEmpty()) {
            return joiner.toString()
        }

        val caregivingRounds = caregivingRoundsByIdsQueryHandler.getCaregivingRounds(
            CaregivingRoundsByIdsQuery(
                caregivingRoundIds = data.map { it.caregivingRoundId },
                subject,
            )
        ).associateBy { it.id }
        val externalCaregivingOrganizations =
            externalCaregivingOrganizationsByIdsQueryHandler.getExternalCaregivingOrganizations(
                ExternalCaregivingOrganizationsByIdsQuery(
                    externalCaregivingOrganizationIds = caregivingRounds.values.mapNotNull { it.receptionInfo.caregivingManagerInfo.organizationId },
                    subject = subject,
                )
            ).associateBy { it.id }

        data.forEach {
            val caregivingRound = caregivingRounds[it.caregivingRoundId]
            val organization = caregivingRound?.receptionInfo?.caregivingManagerInfo?.organizationId.let {
                externalCaregivingOrganizations[it]
            }
            joiner.add(generateDataRecord(caregivingRound, organization, it))
        }

        return joiner.toString()
    }

    private fun generateDataRecord(
        caregivingRound: CaregivingRound?,
        organization: ExternalCaregivingOrganization?,
        reconciliation: Reconciliation,
    ): String {
        val joiner = StringJoiner(",")

        joiner.add(caregivingRound?.receptionInfo?.accidentNumber ?: "-")
        joiner.add(caregivingRound?.receptionInfo?.maskedPatientName ?: "-")
        joiner.add(caregivingRound?.caregivingRoundNumber?.toString() ?: "-")
        joiner.add(reconciliation.billingAmount.toString())
        joiner.add(caregivingRound?.caregiverInfo?.name ?: "-")
        joiner.add(reconciliation.settlementAmount.toString())
        joiner.add(reconciliation.settlementWithdrawalAmount.toString())
        joiner.add(reconciliation.settlementDepositAmount.toString())
        joiner.add(reconciliation.profit.toString())
        joiner.add((reconciliation.profit - reconciliation.distributedProfit).toString())
        joiner.add(organization?.name ?: "-")
        joiner.add(reconciliation.distributedProfit.toString())

        return joiner.toString()
    }
}
