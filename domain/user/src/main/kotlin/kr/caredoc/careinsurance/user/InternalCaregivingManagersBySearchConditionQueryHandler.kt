package kr.caredoc.careinsurance.user

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface InternalCaregivingManagersBySearchConditionQueryHandler {
    fun getInternalCaregivingManagers(query: InternalCaregivingManagersBySearchConditionQuery, pageRequest: Pageable): Page<InternalCaregivingManager>
}
