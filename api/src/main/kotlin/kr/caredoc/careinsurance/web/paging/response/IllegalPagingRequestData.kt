package kr.caredoc.careinsurance.web.paging.response

data class IllegalPagingRequestData(
    val enteredPageSize: Int,
    val enteredPageNumber: Int,
)
