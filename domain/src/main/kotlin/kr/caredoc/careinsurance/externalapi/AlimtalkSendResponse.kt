package kr.caredoc.careinsurance.externalapi

data class AlimtalkSendResponse(
    val data: ResponseData
) {
    data class ResponseData(
        val trackingId: Int,
    )
}
