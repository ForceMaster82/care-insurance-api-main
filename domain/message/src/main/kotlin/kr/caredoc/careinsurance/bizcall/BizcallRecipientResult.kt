package kr.caredoc.careinsurance.bizcall

data class BizcallRecipientResult(
    val validationResultList: List<ValidationResult>,
    val isValid: Boolean,
) {
    data class ValidationResult(
        val errorCode: String,
        val errorMessage: String,
        val references: List<Reference>,
    ) {
        data class Reference(
            val reference: String,
            val target: String,
        )
    }
}
