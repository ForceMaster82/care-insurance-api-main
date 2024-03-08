package kr.caredoc.careinsurance.web.search

class IllegalSearchQueryException(
    val enteredSearchQuery: String,
) : RuntimeException("검색 조건을 해석할 수 없습니다.")
