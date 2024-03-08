package kr.caredoc.careinsurance.patient

import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import kr.caredoc.careinsurance.decodeHex
import kr.caredoc.careinsurance.phonenumber.PartialEncryptedPhoneNumber
import kr.caredoc.careinsurance.security.encryption.Decryptor
import kr.caredoc.careinsurance.security.encryption.Encryptor
import kr.caredoc.careinsurance.security.hash.PepperedHasher
import kr.caredoc.careinsurance.toHex

@Embeddable
data class EncryptedPatientInfo(
    @Embedded
    @AttributeOverrides(
        AttributeOverride(
            name = "hashed",
            column = Column(name = "hashed_patient_name")
        ),
        AttributeOverride(
            name = "encrypted",
            column = Column(name = "encrypted_patient_name")
        ),
        AttributeOverride(
            name = "masked",
            column = Column(name = "masked_patient_name")
        ),
    )
    val name: EncryptedPatientName,
    val nickname: String?,
    val age: Int,
    val height: Int?,
    val weight: Int?,
    @Enumerated(EnumType.STRING)
    val sex: Sex,
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "maskedPhoneNumber", column = Column(name = "patient_primary_phone_number")),
        AttributeOverride(
            name = "encryptedPart",
            column = Column(name = "patient_primary_phone_number_encrypted_part")
        ),
        AttributeOverride(
            name = "relationshipWithPatient",
            column = Column(name = "relationship_with_patient_primary_contact")
        ),
    )
    val primaryContact: EncryptedContact,
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "maskedPhoneNumber", column = Column(name = "patient_secondary_phone_number")),
        AttributeOverride(
            name = "encryptedPart",
            column = Column(name = "patient_secondary_phone_number_encrypted_part")
        ),
        AttributeOverride(
            name = "relationshipWithPatient",
            column = Column(name = "relationship_with_patient_secondary_contact")
        ),
    )
    val secondaryContact: EncryptedContact?,
) {
    @Embeddable
    class EncryptedContact(
        @Access(AccessType.FIELD)
        val maskedPhoneNumber: String,
        val encryptedPart: String,
        val relationshipWithPatient: String,
    ) {
        companion object {
            val Empty = EncryptedContact("", "", "")
        }

        constructor(
            partialEncryptedPhoneNumber: PartialEncryptedPhoneNumber,
            relationshipWithPatient: String,
        ) : this(
            partialEncryptedPhoneNumber.maskedPhoneNumber,
            partialEncryptedPhoneNumber.encryptedSecond.toHex(),
            relationshipWithPatient,
        ) {
            this.internalPartialEncryptedPhoneNumber = partialEncryptedPhoneNumber
        }

        @Transient
        private var internalPartialEncryptedPhoneNumber: PartialEncryptedPhoneNumber? = null

        val partialEncryptedPhoneNumber: PartialEncryptedPhoneNumber
            get() = internalPartialEncryptedPhoneNumber ?: run {
                val partialEncryptedPhoneNumber = if (this.encryptedPart == "") {
                    PartialEncryptedPhoneNumber.Empty
                } else {
                    PartialEncryptedPhoneNumber.encrypt(
                        this.maskedPhoneNumber,
                        this.encryptedPart.decodeHex()
                    )
                }
                this.internalPartialEncryptedPhoneNumber = partialEncryptedPhoneNumber
                partialEncryptedPhoneNumber
            }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as EncryptedContact

            if (maskedPhoneNumber != other.maskedPhoneNumber) return false
            if (encryptedPart != other.encryptedPart) return false
            if (relationshipWithPatient != other.relationshipWithPatient) return false

            return true
        }

        override fun hashCode(): Int {
            var result = maskedPhoneNumber.hashCode()
            result = 31 * result + encryptedPart.hashCode()
            result = 31 * result + relationshipWithPatient.hashCode()
            return result
        }
    }

    @Embeddable
    class EncryptedPatientName(
        @Access(AccessType.FIELD)
        val hashed: String,
        val masked: String,
        val encrypted: String,
    ) {
        @Transient
        private var decrypted: String? = null

        companion object {
            private fun mask(plain: String): String {
                if (plain.isEmpty() && plain.length == 1) {
                    return plain
                }

                val chars = plain.toCharArray()
                chars[1] = '*'
                return String(chars)
            }

            val Empty = EncryptedPatientName("", "", "")
        }

        constructor(
            plain: String,
            hasher: PepperedHasher,
            encryptor: Encryptor,
        ) : this(
            hasher.hash(plain.toByteArray()).toHex(),
            mask(plain),
            encryptor.encrypt(plain).toHex(),
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as EncryptedPatientName

            return hashed == other.hashed
        }

        override fun hashCode(): Int {
            return hashed.hashCode()
        }

        fun decrypt(decryptor: Decryptor): String {
            val decrypted = decryptor.decryptAsString(encrypted.decodeHex())

            this.decrypted = decrypted

            return decrypted
        }

        override fun toString(): String {
            return decrypted ?: masked
        }
    }
}
