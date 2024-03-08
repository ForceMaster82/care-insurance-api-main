package kr.caredoc.careinsurance.billing.revision

import org.springframework.data.jpa.repository.JpaRepository

interface BillingRevisionRepository : JpaRepository<BillingRevision, String>
