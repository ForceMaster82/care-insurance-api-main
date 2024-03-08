package kr.caredoc.careinsurance.web.response

data class GeneralErrorResponse<T>(
    val message: String,
    val errorType: String,
    val data: T,
)
