package kr.caredoc.careinsurance.phonenumber

import kr.caredoc.careinsurance.security.encryption.Decryptor
import kr.caredoc.careinsurance.security.encryption.Encryptor

class PartialEncryptedPhoneNumber(
    private val first: String,
    private var plainSecond: String?,
    private val maskedSecond: String,
    val encryptedSecond: ByteArray,
    private val third: String,
) {
    var unmasked: Boolean = false
        private set

    private val second: String
        get() = if (unmasked) {
            plainSecond ?: maskedSecond
        } else {
            maskedSecond
        }

    constructor(first: String, second: String, third: String, encryptor: Encryptor) : this(
        first = first,
        plainSecond = second,
        maskedSecond = generateSequence { "*" }.take(second.length).joinToString(""),
        encryptedSecond = encryptor.encrypt(second),
        third = third,
    )

    constructor(first: String, maskedSecond: String, encryptedSecond: ByteArray, third: String) : this(
        first = first,
        plainSecond = null,
        maskedSecond = maskedSecond,
        encryptedSecond = encryptedSecond,
        third = third,
    )

    companion object {
        private val SEOUL_PHONE_NUMBER_PATTERN = Regex("""^(\d{2})([\d*]{3,4})(\d{4})$""")
        private val PHONE_NUMBER_PATTERN = Regex("""^(\d{3})([\d*]{3,4})(\d{4})$""")
        private val NATIONAL_REPRESENTATIVE_NUMBER_PATTERN = Regex("""^(\d{0})([\d*]{4})(\d{4})$""")

        fun encrypt(rawPhoneNumber: String, encryptor: Encryptor): PartialEncryptedPhoneNumber {
            val matchResult = if (rawPhoneNumber.isSeoulPhoneNumber) {
                SEOUL_PHONE_NUMBER_PATTERN.matchEntire(rawPhoneNumber) ?: throw InvalidPhoneNumberException(
                    rawPhoneNumber
                )
            } else if (rawPhoneNumber.isNationalRepresentativeNumber) {
                NATIONAL_REPRESENTATIVE_NUMBER_PATTERN.matchEntire(rawPhoneNumber) ?: throw InvalidPhoneNumberException(
                    rawPhoneNumber
                )
            } else {
                PHONE_NUMBER_PATTERN.matchEntire(rawPhoneNumber) ?: throw InvalidPhoneNumberException(rawPhoneNumber)
            }
            val groups = matchResult.groups

            return PartialEncryptedPhoneNumber(
                groups[1]?.value ?: throw IllegalArgumentException(),
                groups[2]?.value ?: throw IllegalArgumentException(),
                groups[3]?.value ?: throw IllegalArgumentException(),
                encryptor,
            )
        }

        fun encrypt(maskedPhoneNumber: String, cipherPart: ByteArray): PartialEncryptedPhoneNumber {
            val matchResult = if (maskedPhoneNumber.isSeoulPhoneNumber) {
                SEOUL_PHONE_NUMBER_PATTERN.matchEntire(maskedPhoneNumber) ?: throw IllegalArgumentException()
            } else if (maskedPhoneNumber.isNationalRepresentativeNumber) {
                NATIONAL_REPRESENTATIVE_NUMBER_PATTERN.matchEntire(maskedPhoneNumber)
                    ?: throw IllegalArgumentException()
            } else {
                PHONE_NUMBER_PATTERN.matchEntire(maskedPhoneNumber) ?: throw IllegalArgumentException()
            }
            val groups = matchResult.groups

            return PartialEncryptedPhoneNumber(
                groups[1]?.value ?: throw IllegalArgumentException(),
                groups[2]?.value ?: throw IllegalArgumentException(),
                cipherPart,
                groups[3]?.value ?: throw IllegalArgumentException(),
            )
        }

        private val String.isSeoulPhoneNumber: Boolean
            get() = this.startsWith("02")
        private val String.isNationalRepresentativeNumber: Boolean
            get() = (this.startsWith("1") || this.startsWith("*")) && this.length == 8

        val Empty = PartialEncryptedPhoneNumber("", "", "", byteArrayOf(), "")
    }

    fun decrypt(decryptor: Decryptor) {
        if (this.encryptedSecond.isEmpty()) {
            this.plainSecond = ""
        } else {
            this.plainSecond = decryptor.decryptAsString(this.encryptedSecond)
        }
        this.unmasked = true
    }

    override fun toString() = "$first$second$third"

    val maskedPhoneNumber: String = "$first$maskedSecond$third"
}
