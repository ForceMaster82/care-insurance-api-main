package kr.caredoc.careinsurance.user.authorizationlogging

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import kr.caredoc.careinsurance.BaseEntity
import java.time.LocalDateTime

@Entity
class UserAuthorizationLogging(
    id: String,
    val grantedUserId: String,
    @Enumerated(EnumType.STRING)
    val grantedRoles: GrantedRoles,
    @Enumerated(EnumType.STRING)
    val grantedType: GrantedType,
    val granterId: String?,
    val grantedDateTime: LocalDateTime,
) : BaseEntity(id)
