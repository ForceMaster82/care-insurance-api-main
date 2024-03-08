package kr.caredoc.careinsurance.coverage

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CoveragesBySearchConditionQueryHandler {
    fun getCoverages(query: CoveragesBySearchConditionQuery, pageRequest: Pageable): Page<Coverage>
}
