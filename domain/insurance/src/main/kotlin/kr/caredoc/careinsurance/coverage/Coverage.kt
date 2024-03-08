package kr.caredoc.careinsurance.coverage

import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.CollectionTable
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import kr.caredoc.careinsurance.BaseEntity
import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.thenThrows
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.time.LocalDateTime

@Entity
class Coverage(
    id: String,
    name: String,
    targetSubscriptionYear: Int,
    @Enumerated(EnumType.STRING)
    val renewalType: RenewalType,
    annualCoveredCaregivingCharges: List<AnnualCoveredCaregivingCharge>,
) : BaseEntity(id), Object {
    var name = name
        protected set
    var targetSubscriptionYear = targetSubscriptionYear
        protected set

    var lastModifiedDateTime: LocalDateTime = Clock.now()
        protected set

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "annual_covered_caregiving_charge")
    @Fetch(FetchMode.SUBSELECT)
    @BatchSize(size = 100)
    protected val internalAnnualCoveredCaregivingCharges = annualCoveredCaregivingCharges.toMutableList()

    val annualCoveredCaregivingCharges: List<AnnualCoveredCaregivingCharge>
        get() = internalAnnualCoveredCaregivingCharges.toList()

    init {
        ensureAnnualCoverageNotDuplicated(annualCoveredCaregivingCharges)
    }

    fun editMetaData(command: CoverageEditingCommand) {
        ensureAnnualCoverageNotDuplicated(command.annualCoveredCaregivingCharges)
        CoverageAccessPolicy.check(command.subject, command, this)

        trackModification {
            this.name = command.name
            this.targetSubscriptionYear = command.targetSubscriptionYear
            this.internalAnnualCoveredCaregivingCharges.clear()
            this.internalAnnualCoveredCaregivingCharges.addAll(command.annualCoveredCaregivingCharges)
        }.ifChanged {
            lastModifiedDateTime = Clock.now()
        }
    }

    @Embeddable
    data class AnnualCoveredCaregivingCharge(
        @Access(AccessType.FIELD)
        val targetAccidentYear: Int,
        val caregivingCharge: Int,
    )

    private fun ensureAnnualCoverageNotDuplicated(annualCoverages: Collection<AnnualCoveredCaregivingCharge>) =
        annualCoverages.groupingBy { it.targetAccidentYear }.eachCount()
            .let { eachCount ->
                eachCount.any { it.value > 1 }.thenThrows {
                    throw AnnualCoverageDuplicatedException(
                        eachCount.asSequence()
                            .filter { it.value > 1 }
                            .map { it.key }.toSet()
                    )
                }
            }

    private fun trackModification(block: () -> Unit): Modification<TrackedData> {
        val tracker = ModificationTracker()

        block()

        return tracker.getModification()
    }

    private inner class ModificationTracker {
        val previous = generateTrackedData()

        fun generateTrackedData() = TrackedData(
            name,
            targetSubscriptionYear,
            internalAnnualCoveredCaregivingCharges.toSet()
        )

        fun getModification() = Modification(previous, generateTrackedData())
    }

    private data class TrackedData(
        val name: String,
        val targetSubscriptionYear: Int,
        val annualCoveredCaregivingCharges: Set<AnnualCoveredCaregivingCharge>,
    )
}
