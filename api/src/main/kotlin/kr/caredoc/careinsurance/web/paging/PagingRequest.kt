package kr.caredoc.careinsurance.web.paging

import java.beans.ConstructorProperties

data class PagingRequest @ConstructorProperties("page-size", "page-number") constructor(
    val pageSize: Int,
    val pageNumber: Int
)
