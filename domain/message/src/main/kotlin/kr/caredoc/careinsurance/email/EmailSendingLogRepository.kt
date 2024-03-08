package kr.caredoc.careinsurance.email

import org.springframework.data.jpa.repository.JpaRepository

interface EmailSendingLogRepository : JpaRepository<EmailSendingLog, String>
