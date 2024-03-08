package kr.caredoc.careinsurance.web.paging

import kr.caredoc.careinsurance.web.paging.exception.IllegalPagingRequestException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

fun PagingRequest.intoPageable(): Pageable = try {
    PageRequest.of(
        pageNumber - 1,
        pageSize,
    )
} catch (e: IllegalArgumentException) {
    throw IllegalPagingRequestException(enteredPageSize = pageSize, enteredPageNumber = pageNumber)
}
