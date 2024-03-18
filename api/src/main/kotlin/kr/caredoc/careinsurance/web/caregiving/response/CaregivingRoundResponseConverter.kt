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
                patientNickName = reception.patientInfo.nickname,
                patientAge = reception.patientInfo.age,
                patientSex = reception.patientInfo.sex,
                patientPrimaryPhoneNumber = reception.patientInfo.primaryContact.maskedPhoneNumber,
                hospitalAndRoom = reception.accidentInfo.hospitalAndRoomInfo.hospitalAndRoom,		//병원
                patientDescription = reception.accidentInfo.patientDescription,		//환자상태
                receivedDateTime = reception.receivedDateTime,		//접수일
                //coverageId = insuranceInfo.coverageId,				//가입담보
                expectedCaregivingStartDate = reception.expectedCaregivingStartDate,
                receptionProgressingStatus = reception.progressingStatus,
                caregivingManagerInfo = DetailCaregivingRoundResponse.CaregivingManagerInfo(
                    organizationType = caregivingRound.receptionInfo.caregivingManagerInfo.organizationType,
                    organizationId = caregivingRound.receptionInfo.caregivingManagerInfo.organizationId,
                    managingUserId = caregivingRound.receptionInfo.caregivingManagerInfo.managingUserId,
                )
            ),
            remarks = caregivingRound.remarks,
        )
    }

    fun intoSimpleResponse(reception: Reception, caregivingRound: CaregivingRound) = SimpleCaregivingRoundResponse(
        id = caregivingRound.id,
        caregivingRoundNumber = caregivingRound.caregivingRoundNumber,
        caregiverName = caregivingRound.caregivingStateData.caregiverInfo?.name,
        caregiverPhoneNumber = caregivingRound.caregivingStateData.caregiverInfo?.phoneNumber.toString(),
        caregiverSex = caregivingRound.caregivingStateData.caregiverInfo?.sex.toString(),
        caregiverBirthDate = caregivingRound.caregivingStateData.caregiverInfo?.birthDate,
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
            patientNickName = reception.patientInfo.nickname,
            patientAge = reception.patientInfo.age,
            patientSex = reception.patientInfo.sex,
            patientPrimaryPhoneNumber = reception.patientInfo.primaryContact.maskedPhoneNumber,
            hospitalAndRoom = reception.accidentInfo.hospitalAndRoomInfo.hospitalAndRoom,		//병원
            patientDescription = reception.accidentInfo.patientDescription,		//환자상태
            receivedDateTime = reception.receivedDateTime,		//접수일
            //coverageId = insuranceInfo.coverageId,				//가입담보
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
