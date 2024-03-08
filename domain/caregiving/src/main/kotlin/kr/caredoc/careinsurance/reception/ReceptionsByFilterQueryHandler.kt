package kr.caredoc.careinsurance.reception

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ReceptionsByFilterQueryHandler {
    fun getReceptions(query: ReceptionsByFilterQuery, pageRequest: Pageable): Page<Reception>
}
