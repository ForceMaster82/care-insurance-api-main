package kr.caredoc.careinsurance.coverage

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface AllCoveragesQueryHandler {
    fun getCoverages(query: AllCoveragesQuery, pageRequest: Pageable): Page<Coverage>
}
