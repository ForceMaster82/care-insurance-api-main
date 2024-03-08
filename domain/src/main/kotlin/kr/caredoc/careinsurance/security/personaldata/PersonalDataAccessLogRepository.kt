package kr.caredoc.careinsurance.security.personaldata

import org.springframework.data.jpa.repository.JpaRepository

interface PersonalDataAccessLogRepository : JpaRepository<PersonalDataAccessLog, String>
