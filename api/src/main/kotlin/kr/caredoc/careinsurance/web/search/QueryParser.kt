package kr.caredoc.careinsurance.web.search

import kr.caredoc.careinsurance.search.SearchCondition

class QueryParser<PROPERTY>(
    private val propertyMap: Map<String, PROPERTY>,
) {
    companion object {
        private val QUERY_PATTERN = Regex("""^([^:]+):(.+)$""")
    }

    fun parse(query: String): SearchCondition<PROPERTY> {
        val result = QUERY_PATTERN.find(query)
            ?: throw IllegalSearchQueryException(query)
        val groups = result.groupValues
        return SearchCondition(
            searchingProperty = propertyMap[groups[1]]
                ?: throw IllegalSearchQueryException(query),
            keyword = groups[2]
        )
    }
}
