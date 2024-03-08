package kr.caredoc.careinsurance.web.paging

import org.springframework.data.domain.Page

fun <T> Page<T>.intoPagedResponse() = PagedResponse(
    currentPageNumber = this.pageable.pageNumber + 1,
    lastPageNumber = this.totalPages,
    totalItemCount = this.totalElements,
    items = this.content
)
