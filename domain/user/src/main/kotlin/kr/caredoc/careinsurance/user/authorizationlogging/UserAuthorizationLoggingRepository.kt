package kr.caredoc.careinsurance.user.authorizationlogging

import org.springframework.data.jpa.repository.JpaRepository

interface UserAuthorizationLoggingRepository : JpaRepository<UserAuthorizationLogging, String>
