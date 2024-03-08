package kr.caredoc.careinsurance.web.caregiving.response

import kr.caredoc.careinsurance.caregiving.CaregivingRound
import kr.caredoc.careinsurance.intoUtcOffsetDateTime
import kr.caredoc.careinsurance.reception.Reception
object CaregivingRoundResponseConverter {
    fun intoDetailResponse(reception: Reception, caregivingRound: CaregivingRound): DetailCaregivingRoundResponse {
        return DetailCaregivingRoundResponse(
            id = caregivingRound.id,
            caregivingRoundNumber = caregivingRound.caregivingRoundNumber,
            caregivingProgressingStatus = caregivingRound.caregivingProgressingStatus,
            startDateTime = caregivingRound.startDateTime?.intoUtcOffsetDateTime(),
            endDateTime = caregivingRound.endDateTime?.intoUtcOffsetDateTime(),
            caregivingRoundClosingReasonType = caregivingRound.caregivingRoundClosingReasonType,
            caregivingRoundClosingReasonDetail = caregivingRound.caregivingRoundClosingReasonDetail,
            cancelDateTime = caregivingRound.cancelDateTime?.intoUtcOffsetDateTime(),
            billingProgressingStatus = caregivingRound.billingProgressingStatus,
            settlementProgressingStatus = caregivingRound.settlementProgressingStatus,
            caregiverInfo = caregivingRound.caregiverInfo?.let { caregiverInfo ->
                DetailCaregivingRoundResponse.CaregiverInfo(
                    caregiverOrganizationId = caregiverInfo.caregiverOrganizationId,
                    name = caregiverInfo.name,
                    sex = caregiverInfo.sex,
                    birthDate = caregiverInfo.birthDate,
                    phoneNumber = caregiverInfo.phoneNumber,
                    insured = caregiverInfo.insured,
                    dailyCaregivingCharge = caregiverInfo.dailyCaregivingCharge,
                    commissionFee = caregiverInfo.commissionFee,
                    accountInfo = DetailCaregivingRoundResponse.AccountInfo(
                        bank = caregiverInfo.accountInfo.bank,
                        accountHolder = caregiverInfo.accountInfo.accountHolder,
                        accountNumber = caregiverInfo.accountInfo.accountNumber,
                    )
                )
            },
            receptionInfo = DetailCaregivingRoundResponse.ReceptionInfo(
                receptionId = reception.id,
                insuranceNumber = reception.insuranceInfo.insuranceNumber,
                accidentNumber = reception.accidentInfo.accidentNumber,
                patientName = reception.patientInfo.name.masked,
                expectedCaregivingStartDate = reception.expectedCaregivingStartDate,
                receptionProgressingStatus = reception.progressingStatus,
                caregivingManagerInfo = DetailCaregivingRoundResponse.CaregivingManagerInfo(
                    organizationType = caregivingRound.receptionInfo.caregivingManagerInfo.organizationType,
                    organizationId = caregivingRound.receptionInfo.caregivingManagerInfo.organizationId,
                    managingUserId = caregivingRound.receptionInfo.caregivingManagerInfo.managingUserId
                )
            ),
            remarks = caregivingRound.remarks,
        )
    }

    fun intoSimpleResponse(reception: Reception, caregivingRound: CaregivingRound) = SimpleCaregivingRoundResponse(
        id = caregivingRound.id,
        caregivingRoundNumber = caregivingRound.caregivingRoundNumber,
        caregiverName = caregivingRound.caregivingStateData.caregiverInfo?.name,
        startDateTime = caregivingRound.startDateTime?.intoUtcOffsetDateTime(),
        endDateTime = caregivingRound.endDateTime?.intoUtcOffsetDateTime(),
        caregivingProgressingStatus = caregivingRound.caregivingProgressingStatus,
        settlementProgressingStatus = caregivingRound.settlementProgressingStatus,
        billingProgressingStatus = caregivingRound.billingProgressingStatus,
        receptionInfo = SimpleCaregivingRoundResponse.ReceptionInfo(
            receptionId = caregivingRound.receptionInfo.receptionId,
            insuranceNumber = caregivingRound.receptionInfo.insuranceNumber,
            accidentNumber = caregivingRound.receptionInfo.accidentNumber,
            patientName = reception.patientInfo.name.masked,
            expectedCaregivingStartDate = caregivingRound.receptionInfo.expectedCaregivingStartDate,
            receptionProgressingStatus = caregivingRound.receptionInfo.receptionProgressingStatus,
            caregivingManagerInfo = SimpleCaregivingRoundResponse.CaregivingManagerInfo(
                organizationType = caregivingRound.receptionInfo.caregivingManagerInfo.organizationType,
                organizationId = caregivingRound.receptionInfo.caregivingManagerInfo.organizationId,
                managingUserId = caregivingRound.receptionInfo.caregivingManagerInfo.managingUserId
            )
        ),
    )
}
