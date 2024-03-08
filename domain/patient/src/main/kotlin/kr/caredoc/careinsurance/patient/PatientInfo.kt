package kr.caredoc.careinsurance.patient

data class PatientInfo(
    val name: String,
    val nickname: String?,
    val age: Int,
    val height: Int?,
    val weight: Int?,
    val sex: Sex,
    val primaryContact: Contact,
    val secondaryContact: Contact?,
) {
    data class Contact(
        val phoneNumber: String,
        val relationshipWithPatient: String,
    )
}
