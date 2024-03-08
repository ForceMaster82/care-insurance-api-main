package kr.caredoc.careinsurance.coverage

interface CoverageByIdQueryHandler {
    fun <T> getCoverage(
        query: CoverageByIdQuery,
        mapper: (Coverage) -> T
    ): T

    fun ensureCoverageExists(query: CoverageByIdQuery)
}
