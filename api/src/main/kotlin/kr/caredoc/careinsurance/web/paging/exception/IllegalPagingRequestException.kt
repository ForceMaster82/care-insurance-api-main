package kr.caredoc.careinsurance.web.paging.exception

class IllegalPagingRequestException(val enteredPageSize: Int, val enteredPageNumber: Int) : RuntimeException(
    "잘못된 페이징 요청입니다.(pageSize: $enteredPageSize, pageNumber: $enteredPageNumber)"
)
