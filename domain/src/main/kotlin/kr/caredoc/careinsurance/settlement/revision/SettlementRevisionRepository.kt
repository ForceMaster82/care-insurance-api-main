package kr.caredoc.careinsurance.settlement.revision

import org.springframework.data.jpa.repository.JpaRepository

interface SettlementRevisionRepository : JpaRepository<SettlementRevision, String>
