package kr.caredoc.careinsurance.search

data class SearchCondition<PROPERTY>(
    val searchingProperty: PROPERTY,
    val keyword: String,
)
