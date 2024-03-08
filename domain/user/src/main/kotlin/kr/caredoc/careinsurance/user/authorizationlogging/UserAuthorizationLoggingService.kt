package kr.caredoc.careinsurance.user.authorizationlogging

import com.github.guepardoapps.kulid.ULID
import kr.caredoc.careinsurance.security.accesscontrol.SubjectAttribute
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerByIdQuery
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerByIdQueryHandler
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerByUserIdQuery
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerByUserIdQueryHandler
import kr.caredoc.careinsurance.user.ExternalCaregivingManagerGenerated
import kr.caredoc.careinsurance.user.InternalCaregivingManagerByIdQuery
import kr.caredoc.careinsurance.user.InternalCaregivingManagerByIdQueryHandler
import kr.caredoc.careinsurance.user.InternalCaregivingManagerByUserIdQuery
import kr.caredoc.careinsurance.user.InternalCaregivingManagerByUserIdQueryHandler
import kr.caredoc.careinsurance.user.InternalCaregivingManagerGenerated
import kr.caredoc.careinsurance.user.UserModified
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserAuthorizationLoggingService(
    val userAuthorizationLoggingRepository: UserAuthorizationLoggingRepository,
    val externalCaregivingManagerByIdQueryHandler: ExternalCaregivingManagerByIdQueryHandler,
    val internalCaregivingManagerByIdQueryHandler: InternalCaregivingManagerByIdQueryHandler,
    val externalCaregivingManagerByUserIdQueryHandler: ExternalCaregivingManagerByUserIdQueryHandler,
    val internalCaregivingManagerByUserIdQueryHandler: InternalCaregivingManagerByUserIdQueryHandler,
) {
    @Transient
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    @EventListener(ExternalCaregivingManagerGenerated::class)
    fun handleExternalCaregivingManagerGenerated(event: ExternalCaregivingManagerGenerated) {
        val granterId = event.subject[SubjectAttribute.USER_ID].firstOrNull()
        if (granterId == null) {
            logger.warn("제휴사 사용자(${event.externalCaregivingManagerId})를 생성한 주체의 사용자 아이디를 특정할 수 없습니다.")
        }

        externalCaregivingManagerByIdQueryHandler.getExternalCaregivingManager(
            ExternalCaregivingManagerByIdQuery(event.externalCaregivingManagerId, event.subject)
        )
        userAuthorizationLoggingRepository.save(
            UserAuthorizationLogging(
                id = ULID.random(),
                grantedUserId = event.userId,
                grantedRoles = GrantedRoles.EXTERNAL_USER,
                grantedType = GrantedType.GRANTED,
                granterId = granterId,
                grantedDateTime = event.grantedDateTime,
            )
        )
    }

    @Transactional
    @EventListener(InternalCaregivingManagerGenerated::class)
    fun handleInternalCaregivingManagerGenerated(event: InternalCaregivingManagerGenerated) {
        val granterId = event.subject[SubjectAttribute.USER_ID].firstOrNull()
        if (granterId == null) {
            logger.warn("내부 관리자(${event.internalCaregivingManagerId})를 생성한 주체의 사용자 아이디를 특정할 수 없습니다.")
        }

        internalCaregivingManagerByIdQueryHandler.getInternalCaregivingManager(
            InternalCaregivingManagerByIdQuery(event.internalCaregivingManagerId, event.subject)
        )
        userAuthorizationLoggingRepository.save(
            UserAuthorizationLogging(
                id = ULID.random(),
                grantedUserId = event.userId,
                grantedRoles = GrantedRoles.INTERNAL_USER,
                grantedType = GrantedType.GRANTED,
                granterId = granterId,
                grantedDateTime = event.grantedDateTime,
            )
        )
    }

    @Transactional
    @EventListener(UserModified::class)
    fun handleUserModified(event: UserModified) {
        val granterId = event.editSubject[SubjectAttribute.USER_ID].firstOrNull()
        if (granterId == null) {
            logger.warn("사용자(${event.userId})를 수정한 주체의 사용자 아이디를 특정할 수 없습니다.")
        }

        val existInternalCaregivingManager =
            internalCaregivingManagerByUserIdQueryHandler.existInternalCaregivingManager(
                InternalCaregivingManagerByUserIdQuery(event.userId, event.editSubject)
            )

        val existExternalCaregivingManager =
            externalCaregivingManagerByUserIdQueryHandler.existExternalCaregivingManager(
                ExternalCaregivingManagerByUserIdQuery(event.userId, event.editSubject)
            )

        if (existInternalCaregivingManager && existExternalCaregivingManager) {
            logger.warn("사용자(${event.userId})는 내부 관리자 이면서 제휴사 사용자 입니다.")
        } else if (!existInternalCaregivingManager && !existExternalCaregivingManager) {
            logger.warn("사용자(${event.userId})는 내부 관리자도 아니고 제휴사 사용자도 아닙니다.")
        }

        val grantedRole = when {
            existInternalCaregivingManager -> GrantedRoles.INTERNAL_USER
            existExternalCaregivingManager -> GrantedRoles.EXTERNAL_USER
            else -> GrantedRoles.UNKNOWN_USER
        }

        event.suspended.ifChanged {
            if (event.suspended.current) {
                userAuthorizationLoggingRepository.save(
                    UserAuthorizationLogging(
                        id = ULID.random(),
                        grantedUserId = event.userId,
                        grantedRoles = grantedRole,
                        grantedType = GrantedType.REVOKED,
                        granterId = granterId,
                        grantedDateTime = event.modifiedDateTime,
                    )
                )
            } else {
                userAuthorizationLoggingRepository.save(
                    UserAuthorizationLogging(
                        id = ULID.random(),
                        grantedUserId = event.userId,
                        grantedRoles = grantedRole,
                        grantedType = GrantedType.GRANTED,
                        granterId = granterId,
                        grantedDateTime = event.modifiedDateTime,
                    )
                )
            }
        }
    }
}
