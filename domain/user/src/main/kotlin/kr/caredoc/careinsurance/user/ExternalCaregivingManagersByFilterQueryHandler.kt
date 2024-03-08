package kr.caredoc.careinsurance.user

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ExternalCaregivingManagersByFilterQueryHandler {
    fun getExternalCaregivingManagers(query: ExternalCaregivingManagersByFilterQuery, pageRequest: Pageable): Page<ExternalCaregivingManager>
}
