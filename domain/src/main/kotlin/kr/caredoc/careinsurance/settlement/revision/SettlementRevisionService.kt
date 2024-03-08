package kr.caredoc.careinsurance.settlement.revision

import com.github.guepardoapps.kulid.ULID
import kr.caredoc.careinsurance.settlement.SettlementGenerated
import kr.caredoc.careinsurance.settlement.SettlementModified
import kr.caredoc.careinsurance.settlement.SettlementTransactionRecorded
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SettlementRevisionService(
    private val settlementRevisionRepository: SettlementRevisionRepository,
) {

    @EventListener(SettlementGenerated::class)
    @Transactional
    fun handleSettlementGenerated(event: SettlementGenerated) {
        val revision = SettlementRevision(
            id = ULID.random(),
            settlementId = event.settlementId,
            progressingStatus = event.progressingStatus,
            totalAmount = event.totalAmount,
            totalDepositAmount = 0,
            totalWithdrawalAmount = 0,
        )
        settlementRevisionRepository.save(revision)
    }

    @EventListener(SettlementModified::class)
    @Transactional
    fun handleSettlementModified(event: SettlementModified) {
        settlementRevisionRepository.save(
            SettlementRevision(
                id = ULID.random(),
                settlementId = event.settlementId,
                progressingStatus = event.progressingStatus.current,
                totalAmount = event.totalAmount,
                totalDepositAmount = event.totalDepositAmount,
                totalWithdrawalAmount = event.totalWithdrawalAmount,
            )
        )
    }

    @EventListener(SettlementTransactionRecorded::class)
    @Transactional
    fun handleSettlementTransactionRecorded(event: SettlementTransactionRecorded) {
        settlementRevisionRepository.save(
            SettlementRevision(
                id = ULID.random(),
                settlementId = event.settlementId,
                progressingStatus = event.progressingStatus,
                totalAmount = event.totalAmount,
                totalDepositAmount = event.totalDepositAmount,
                totalWithdrawalAmount = event.totalWithdrawalAmount,
            )
        )
    }
}
