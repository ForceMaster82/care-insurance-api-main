package kr.caredoc.careinsurance

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

fun Pageable.withSort(sort: Sort): Pageable = PageRequest.of(
    this.pageNumber,
    this.pageSize,
    sort,
)
