package kr.caredoc.careinsurance.user

import com.github.guepardoapps.kulid.ULID
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Transient
import kr.caredoc.careinsurance.AggregateRoot
import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.modification.Modification
import kr.caredoc.careinsurance.patch.Patch
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.ObjectAttribute
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.security.accesscontrol.SubjectAttribute
import kr.caredoc.careinsurance.security.password.PasswordPolicy
import kr.caredoc.careinsurance.thenThrows
import kr.caredoc.careinsurance.user.exception.CredentialNotMatchedException
import kr.caredoc.careinsurance.user.exception.UserSuspendedException
import java.security.MessageDigest
import java.time.LocalDateTime
import kotlin.random.Random

@Entity
@Table(name = "`user`")
class User(
    id: String,
    @Embedded
    protected var credential: EmailPasswordCredential,
    name: String,
) : AggregateRoot(id), Subject, Object {

    var suspended = false
        protected set
    var lastLoginDateTime: LocalDateTime = Clock.now()
        protected set

    var name = name
        protected set

    @get:Transient
    val emailAddress: String
        get() = this.credential.emailAddress

    var credentialRevision: String = ULID.random()
        protected set

    // 최초 생성시 패스워드를 반드시 수정하도록 만료된 상태로 시작
    var passwordExpirationDateTime: LocalDateTime? = Clock.now()
        protected set

    @get:Transient
    val passwordExpired: Boolean
        get() = this.passwordExpirationDateTime?.isBefore(Clock.now()) ?: false

    private object PasswordHasher {
        fun hash(plain: String, salt: String): String {
            val sha256 = MessageDigest.getInstance("SHA-256")
            return sha256.digest(plain.toByteArray() + salt.toByteArray()).toHex()
        }

        fun generateRandomSalt(): String = Random.nextBytes(8).toHex()

        private fun ByteArray.toHex() = joinToString("") { "%02X".format(it) }
    }

    private class PasswordNotMatched : RuntimeException()

    @Embeddable
    class EmailPasswordCredential(
        emailAddress: String,
        password: String,
    ) {
        @Access(AccessType.FIELD)
        var emailAddress = emailAddress
            protected set

        @Access(AccessType.FIELD)
        var salt: String = PasswordHasher.generateRandomSalt()

        @Access(AccessType.FIELD)
        var hashedPassword: String = run {
            PasswordPolicy.ensurePasswordLegal(password)
            PasswordHasher.hash(password, salt)
        }

        fun ensurePasswordMatched(password: String) = run {
            PasswordPolicy.ensurePasswordLegal(password)
            PasswordHasher.hash(password, salt) != hashedPassword
        }.thenThrows {
            PasswordNotMatched()
        }

        fun changeEmailAddress(emailAddress: Patch<String>) {
            this.emailAddress = emailAddress.compareWith(this.emailAddress).valueToOverwrite
        }

        fun changePassword(password: String) {
            PasswordPolicy.ensurePasswordLegal(password)
            hashedPassword = PasswordHasher.hash(password, salt)
        }
    }

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "hashedAuthenticationCode", column = Column(name = "hashed_authentication_code")),
        AttributeOverride(name = "salt", column = Column(name = "authentication_code_salt")),
    )
    protected var authenticationCode: AuthenticationCode? = null

    @Embeddable
    protected class AuthenticationCode(rawAuthenticationCode: String) {
        @Access(AccessType.FIELD)
        private val salt = PasswordHasher.generateRandomSalt()
        private val hashedAuthenticationCode = PasswordHasher.hash(rawAuthenticationCode, this.salt)

        fun hasAuthenticationCode(rawAuthenticationCode: String) =
            this.hashedAuthenticationCode == PasswordHasher.hash(rawAuthenticationCode, this.salt)
    }

    fun login(password: String) {
        ensurePasswordMatched(password)
        lastLoginDateTime = Clock.now()
    }

    private fun ensurePasswordMatched(password: String) = try {
        credential.ensurePasswordMatched(password)
    } catch (e: PasswordNotMatched) {
        throw CredentialNotMatchedException(id)
    }

    fun ensureUserActivated() = suspended.thenThrows {
        UserSuspendedException(id)
    }

    override fun get(attribute: SubjectAttribute) = when (attribute) {
        SubjectAttribute.USER_ID -> setOf(id)
        SubjectAttribute.CREDENTIAL_EXPIRED -> setOf(passwordExpired.toString())
        else -> setOf()
    }

    override fun get(attribute: ObjectAttribute) = when (attribute) {
        ObjectAttribute.OWNER_ID -> setOf(id)
        else -> setOf()
    }

    fun edit(command: UserEditingCommand) = trackModification(subject = command.subject) {
        UserAccessPolicy.check(command.subject, command, this)

        this.name = command.name.compareWith(this.name).valueToOverwrite
        this.credential.changeEmailAddress(command.email)
        this.suspended = command.suspended.compareWith(this.suspended).valueToOverwrite
        command.passwordModification?.let { changePassword(it.currentPassword, it.newPassword) }
    }

    private fun changePassword(oldPassword: String, newPassword: String) {
        this.ensurePasswordMatched(oldPassword)
        this.credential.changePassword(newPassword)

        resetPasswordExpiration()

        renewCredentialRevision()
    }

    fun resetPassword(command: UserPasswordResetCommand): PasswordResetResult {
        UserAccessPolicy.check(command.subject, command, this)

        val newPassword = PasswordPolicy.generateRandomPassword()
        this.credential.changePassword(newPassword)

        renewCredentialRevision()

        expirePassword()

        return PasswordResetResult(newPassword)
    }

    fun issueTemporalAuthenticationCode(): AuthenticationCodeIssuingResult {
        val authenticationCode = Random.nextInt(0, 1000000).toString().padStart(6, '0')
        this.authenticationCode = AuthenticationCode(authenticationCode)

        renewCredentialRevision()

        return AuthenticationCodeIssuingResult(authenticationCode)
    }

    fun authenticateUsingCode(authenticationCode: String) {
        if (this.authenticationCode?.hasAuthenticationCode(authenticationCode) == true) {
            return
        }

        throw CredentialNotMatchedException(id)
    }

    private inner class ModificationTracker {
        private val previous = generateTrackData(this@User)

        private fun generateTrackData(status: User) = TrackedData(
            suspended = status.suspended
        )

        private fun generateModifiedEvent(
            current: TrackedData,
            subject: Subject,
        ) = UserModified(
            userId = id,
            suspended = Modification(
                previous.suspended,
                current.suspended
            ),
            editSubject = subject,
        )

        private fun updateUserModificationEvent(
            current: TrackedData,
            subject: Subject
        ) {
            updateEvent(UserModified::class, generateModifiedEvent(current, subject))
        }

        fun updateModifiedEvent(subject: Subject) {
            val current = generateTrackData(this@User)
            if (previous == current) {
                return
            }
            updateUserModificationEvent(current, subject)
        }
    }

    @Transient
    private var modificationTracker: ModificationTracker? = null

    private fun initModificationTracking() {
        if (this.modificationTracker == null) {
            this.modificationTracker = ModificationTracker()
        }
    }

    private fun trackModification(
        subject: Subject,
        block: () -> Unit
    ) {
        initModificationTracking()
        block()
        modificationTracker?.updateModifiedEvent(subject)
    }

    private data class TrackedData(
        val suspended: Boolean,
    )

    override fun clearEvents() {
        super.clearEvents()
        this.modificationTracker = ModificationTracker()
    }

    private fun renewCredentialRevision() {
        this.credentialRevision = ULID.random()
    }

    fun ensureCredentialRevisionMatched(credentialRevision: String) {
        if (this.credentialRevision != credentialRevision) {
            throw CredentialNotMatchedException(id)
        }
    }

    private fun resetPasswordExpiration() {
        // 현재는 패스워드 만료 정책이 없으므로 패스워드 제한 기간을 두지 않음
        this.passwordExpirationDateTime = null
    }

    private fun expirePassword() {
        this.passwordExpirationDateTime = Clock.now()
    }
}
