package kr.caredoc.careinsurance.security.personaldata

import kr.caredoc.careinsurance.patient.EncryptedPatientInfo
import kr.caredoc.careinsurance.patient.PatientInfo
import kr.caredoc.careinsurance.phonenumber.PartialEncryptedPhoneNumber
import kr.caredoc.careinsurance.security.encryption.Encryptor
import kr.caredoc.careinsurance.security.hash.PepperedHasher

class PatientInfoEncryptor(
    private val encryptor: Encryptor,
    private val patientNameHasher: PepperedHasher,
) {
    fun encrypt(plain: PatientInfo) = EncryptedPatientInfo(
        name = EncryptedPatientInfo.EncryptedPatientName(plain.name, patientNameHasher, encryptor),
        nickname = plain.nickname,
        age = plain.age,
        height = plain.height,
        weight = plain.weight,
        sex = plain.sex,
        primaryContact = encrypt(plain.primaryContact),
        secondaryContact = plain.secondaryContact?.let { encrypt(it) },
    )

    fun encrypt(plain: PatientInfo.Contact) = EncryptedPatientInfo.EncryptedContact(
        partialEncryptedPhoneNumber = PartialEncryptedPhoneNumber.encrypt(plain.phoneNumber, encryptor),
        relationshipWithPatient = plain.relationshipWithPatient,
    )
}
