package kr.caredoc.careinsurance.billing.revision

import com.github.guepardoapps.kulid.ULID
import kr.caredoc.careinsurance.billing.BillingByIdQuery
import kr.caredoc.careinsurance.billing.BillingByIdQueryHandler
import kr.caredoc.careinsurance.billing.BillingGenerated
import kr.caredoc.careinsurance.billing.BillingModified
import kr.caredoc.careinsurance.billing.BillingTransactionRecorded
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BillingRevisionService(
    val billingRevisionRepository: BillingRevisionRepository,
    val billingByIdQueryHandler: BillingByIdQueryHandler,
) {
    @Transactional
    @EventListener(BillingGenerated::class)
    fun handleBillingGenerated(event: BillingGenerated) {
        billingByIdQueryHandler.getBilling(
            BillingByIdQuery(event.billingId, event.subject)
        )
        billingRevisionRepository.save(
            BillingRevision(
                id = ULID.random(),
                billingId = event.billingId,
                billingProgressingStatus = event.progressingStatus,
                billingAmount = event.billingAmount,
                totalDepositAmount = 0,
                totalWithdrawalAmount = 0,
                issuedDateTime = event.issuedDateTime,
            )
        )
    }

    @Transactional
    @EventListener(BillingModified::class)
    fun handleBillingModified(event: BillingModified) {
        billingByIdQueryHandler.getBilling(
            BillingByIdQuery(event.billingId, event.subject)
        )

        event.progressingStatus.ifChanged {
            billingRevisionRepository.save(
                BillingRevision(
                    id = ULID.random(),
                    billingId = event.billingId,
                    billingProgressingStatus = event.progressingStatus.current,
                    billingAmount = event.totalAmount.current,
                    totalDepositAmount = event.totalDepositAmount.current,
                    totalWithdrawalAmount = event.totalWithdrawalAmount.current,
                    issuedDateTime = event.modifiedDateTime,
                )
            )
        }
    }

    @Transactional
    @EventListener(BillingTransactionRecorded::class)
    fun handleBillingTransactionRecorded(event: BillingTransactionRecorded) {
        billingByIdQueryHandler.getBilling(
            BillingByIdQuery(event.billingId, event.subject)
        )

        billingRevisionRepository.save(
            BillingRevision(
                id = ULID.random(),
                billingId = event.billingId,
                billingProgressingStatus = event.progressingStatus,
                billingAmount = event.totalAmount,
                totalDepositAmount = event.totalDepositAmount,
                totalWithdrawalAmount = event.totalWithdrawalAmount,
                issuedDateTime = event.enteredDateTime,
            )
        )
    }
}
