package kr.caredoc.careinsurance.reception.modification

import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Entity
import jakarta.persistence.Transient
import kr.caredoc.careinsurance.AggregateRoot
import kr.caredoc.careinsurance.caregiving.CaregivingChargeModified
import kr.caredoc.careinsurance.security.accesscontrol.SubjectAttribute
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Entity
class CaregivingChargeModificationSummary(
    id: String,
    val receptionId: String,
) : AggregateRoot(id) {
    @Transient
    private var logger = LoggerFactory.getLogger(javaClass)

    @Access(AccessType.FIELD)
    var lastModifiedDateTime: LocalDateTime? = null
        protected set
    var lastModifierId: String? = null
        protected set
    var modificationCount: Int = 0
        protected set

    fun handleCaregivingChargeModified(event: CaregivingChargeModified) {
        val modifierId = event.editingSubject[SubjectAttribute.USER_ID].firstOrNull()
        if (modifierId == null) {
            logger.warn("Reception($receptionId)의 간병비 산정을 수정한 주체의 사용자 아이디를 특정할 수 없습니다.")
            return
        }

        if (lastModifiedDateTime?.isBefore(event.calculatedDateTime) != false) {
            lastModifiedDateTime = event.calculatedDateTime
            lastModifierId = modifierId
        }
        modificationCount += 1
    }

    override fun postLoad() {
        super.postLoad()
        this.logger = LoggerFactory.getLogger(javaClass)
    }
}
