package kr.caredoc.careinsurance.user

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface AllInternalCaregivingManagersQueryHandler {
    fun getInternalCaregivingManagers(query: GetAllInternalCaregivingManagersQuery, pageRequest: Pageable): Page<InternalCaregivingManager>
}
