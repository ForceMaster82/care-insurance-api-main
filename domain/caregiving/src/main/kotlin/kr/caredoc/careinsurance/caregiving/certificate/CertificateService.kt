package kr.caredoc.careinsurance.caregiving.certificate

import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationByIdQuery
import kr.caredoc.careinsurance.agency.ExternalCaregivingOrganizationByIdQueryHandler
import kr.caredoc.careinsurance.caregiving.CaregivingChargeByCaregivingRoundIdQuery
import kr.caredoc.careinsurance.caregiving.CaregivingChargeByCaregivingRoundIdQueryHandler
import kr.caredoc.careinsurance.caregiving.CaregivingRound
import kr.caredoc.careinsurance.caregiving.CaregivingRoundByIdQuery
import kr.caredoc.careinsurance.caregiving.CaregivingRoundByIdQueryHandler
import kr.caredoc.careinsurance.caregiving.CaregivingRoundsByReceptionIdQuery
import kr.caredoc.careinsurance.caregiving.CaregivingRoundsByReceptionIdQueryHandler
import kr.caredoc.careinsurance.caregiving.ClosingReasonType
import kr.caredoc.careinsurance.patient.Sex
import kr.caredoc.careinsurance.reception.ReceptionByIdQuery
import kr.caredoc.careinsurance.reception.ReceptionByIdQueryHandler
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.security.encryption.Decryptor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

@Service
class CertificateService(
    private val receptionByIdQueryHandler: ReceptionByIdQueryHandler,
    private val caregivingRoundByIdQueryHandler: CaregivingRoundByIdQueryHandler,
    private val caregivingRoundsByReceptionIdQueryHandler: CaregivingRoundsByReceptionIdQueryHandler,
    private val externalCaregivingOrganizationByIdQueryHandler: ExternalCaregivingOrganizationByIdQueryHandler,
    private val caregivingChargeByCaregivingRoundIdQueryHandler: CaregivingChargeByCaregivingRoundIdQueryHandler,
    private val decryptor: Decryptor,
) : CertificateByCaregivingRoundIdQueryHandler {
    @Transactional
    override fun getCertificate(query: CertificateByCaregivingRoundIdQuery): ByteArray {
        CertificateAccessPolicy.check(query.subject, query, Object.Empty)

        val subject = query.subject
        val caregivingRound = caregivingRoundByIdQueryHandler.getCaregivingRound(
            CaregivingRoundByIdQuery(
                caregivingRoundId = query.caregivingRoundId,
                subject = subject,
            )
        )
        val reception = receptionByIdQueryHandler.getReception(
            ReceptionByIdQuery(
                receptionId = caregivingRound.receptionInfo.receptionId,
                subject = subject,
            )
        )
        val caregivingOrganization = caregivingRound.caregiverInfo?.caregiverOrganizationId?.let {
            externalCaregivingOrganizationByIdQueryHandler.getExternalCaregivingOrganization(
                ExternalCaregivingOrganizationByIdQuery(
                    id = it,
                    subject = subject,
                )
            )
        }

        val caregivingCharge = caregivingChargeByCaregivingRoundIdQueryHandler.getCaregivingCharge(
            CaregivingChargeByCaregivingRoundIdQuery(
                caregivingRoundId = caregivingRound.id,
                subject = query.subject,
            )
        )

        val isLastRound = caregivingRound.isLastRound(subject)
        val cancelAfterArrivedText = if (caregivingCharge.isCancelAfterArrived) {
            " - 도착 후 취소"
        } else {
            ""
        }

        val remarks = when (caregivingRound.caregivingRoundClosingReasonType) {
            ClosingReasonType.FINISHED -> "간병 종료"
            ClosingReasonType.FINISHED_USING_PERSONAL_CAREGIVER -> "간병 종료(개인구인 안내)"
            ClosingReasonType.FINISHED_CONTINUE -> "간병 중"
            ClosingReasonType.FINISHED_RESTARTING -> "간병 중(중단-계속)"
            ClosingReasonType.FINISHED_CHANGING_CAREGIVER -> "간병 중(간병인 교체)"
            ClosingReasonType.FINISHED_CHANGING_HOSPITAL -> "간병 중(병원 전원)"
            ClosingReasonType.FINISHED_CHANGING_CAREGIVER_AND_HOSPITAL -> "간병 중(간병인 교체 및 병원 전원)"
            else -> ""
        } + cancelAfterArrivedText

        val pdfStream = CertificateTemplate.generate(
            CertificateTemplate.CertificateData(
                accidentNumber = reception.accidentInfo.accidentNumber,
                subscriptionDate = reception.insuranceInfo.subscriptionDate,
                patientName = reception.inDecryptionContext(decryptor, subject) { decryptPatientName() },
                patientAge = reception.patientInfo.age,
                patientSex = reception.patientInfo.sex,
                patientPhoneNumber = reception.inDecryptionContext(decryptor, subject) { decryptPrimaryContact() },
                relationshipBetweenPatientAndPhoneOwner = reception.patientInfo.primaryContact.relationshipWithPatient,
                accidentDate = reception.accidentInfo.accidentDateTime.toLocalDate(),
                patientDescription = reception.accidentInfo.patientDescription,
                hospitalState = reception.accidentInfo.hospitalAndRoomInfo.state,
                hospitalCity = reception.accidentInfo.hospitalAndRoomInfo.city,
                hospitalAndRoom = reception.accidentInfo.hospitalAndRoomInfo.hospitalAndRoom,
                caregiverName = caregivingRound.caregiverInfo?.name ?: "-",
                caregiverBirthDate = caregivingRound.caregiverInfo?.birthDate ?: "",
                caregiverSex = caregivingRound.caregiverInfo?.sex ?: Sex.FEMALE,
                caregiverPhoneNumber = caregivingRound.caregiverInfo?.phoneNumber ?: "-",
                caregivingOrganizationName = caregivingOrganization?.name ?: "㈜ 케어닥",
                caregivingOrganizationPhoneNumber = caregivingOrganization?.phoneNumber ?: "1833-9119",
                caregivingStartDateTime = caregivingRound.startDateTime!!,
                caregivingEndDateTime = caregivingRound.endDateTime!!,
                remarks = remarks,
                continueWithSameCaregiver = !isLastRound,
            )
        )

        return convertPdfToJpg(pdfStream)
    }

    private fun CaregivingRound.isLastRound(subject: Subject): Boolean {
        if (
            !setOf(
                ClosingReasonType.FINISHED,
                ClosingReasonType.FINISHED_USING_PERSONAL_CAREGIVER,
            ).contains(this.caregivingRoundClosingReasonType)
        ) {
            return false
        }

        val caregivingRoundsInSameReception = caregivingRoundsByReceptionIdQueryHandler.getReceptionCaregivingRounds(
            CaregivingRoundsByReceptionIdQuery(
                receptionId = this.receptionInfo.receptionId,
                subject = subject,
            )
        )

        val actualLastRound = caregivingRoundsInSameReception.maxByOrNull { it.caregivingRoundNumber }!!

        return actualLastRound.id == this.id
    }
    private fun convertPdfToJpg(pdfStream: ByteArrayOutputStream) = PDFConverter.convertJPG(
        convertByteArrayOutputStreamToInputStream(pdfStream)
    ).toByteArray()

    private fun convertByteArrayOutputStreamToInputStream(stream: ByteArrayOutputStream): InputStream {
        val inputStream = PipedInputStream()
        val outputStream = PipedOutputStream(inputStream)
        Thread {
            outputStream.use { stream.writeTo(outputStream) }
        }.start()
        return inputStream
    }
}
