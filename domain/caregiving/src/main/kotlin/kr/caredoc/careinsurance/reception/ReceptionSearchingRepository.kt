package kr.caredoc.careinsurance.reception

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

interface ReceptionSearchingRepository {
    fun searchReceptions(
        searchingCriteria: SearchingCriteria,
        pageable: Pageable,
    ): Page<Reception>

    data class SearchingCriteria(
        val from: LocalDate? = null,
        val until: LocalDate? = null,
        val urgency: Reception.Urgency? = null,
        val periodType: Reception.PeriodType? = null,
        val caregivingManagerAssigned: Boolean? = null,
        val organizationType: OrganizationType? = null,
        val progressingStatuses: Collection<ReceptionProgressingStatus> = setOf(),
        val insuranceNumberContains: String? = null,
        val patientNameContains: String? = null,
        val patientPhoneNumberContains: String? = null,
        val accidentNumberContains: String? = null,
        val managerNameContains: String? = null,
        val caregiverName: String? = null,
        val hospitalAndRoom: String? = null,
    )
}
