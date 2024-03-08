package kr.caredoc.careinsurance.aws.ses

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
import com.amazonaws.services.simpleemail.model.AmazonSimpleEmailServiceException
import com.amazonaws.services.simpleemail.model.Body
import com.amazonaws.services.simpleemail.model.Content
import com.amazonaws.services.simpleemail.model.Destination
import com.amazonaws.services.simpleemail.model.Message
import com.amazonaws.services.simpleemail.model.SendEmailRequest
import com.github.guepardoapps.kulid.ULID
import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.email.Email
import kr.caredoc.careinsurance.email.EmailSender
import kr.caredoc.careinsurance.email.EmailSendingLog
import kr.caredoc.careinsurance.email.EmailSendingLogRepository
import kr.caredoc.careinsurance.email.Senders
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class SesEmailSender(
    //private val sesClient: AmazonSimpleEmailService,
    private val senders: Senders,
    private val emailSendingLogRepository: EmailSendingLogRepository,
) : EmailSender {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional(
        propagation = Propagation.REQUIRES_NEW,
        noRollbackFor = [AmazonSimpleEmailServiceException::class],
    )
    override fun send(email: Email) {
        val request = SendEmailRequest()
            .withMessage(
                Message()
                    .withSubject(email.mailSubject)
                    .withBody(email.body)
            )
            .withDestination(email.destination)
            .withSource(senders[email.senderProfile])

        try {
           // sesClient.sendEmail(request)
        } catch (e: AmazonSimpleEmailServiceException) {
            emailSendingLogRepository.save(EmailSendingLog.ofFailed(email, e))
            logger.error("failed to send email. from: ${senders[email.senderProfile]} to: ${email.recipient}", e)
            throw e
        }

        emailSendingLogRepository.save(EmailSendingLog.ofSent(email))
    }

    private val Email.mailSubject: Content
        get() = Content().withData(this.title)

    private val Email.body: Body
        get() = Body().withText(Content().withData(this.content))

    private val Email.destination: Destination
        get() = Destination().withToAddresses(this.recipient)

    private fun EmailSendingLog.Companion.ofSent(email: Email) = ofSent(
        id = ULID.random(),
        recipientAddress = email.recipient,
        senderAddress = senders[email.senderProfile],
        senderProfile = email.senderProfile.name,
        title = email.title,
        sentDateTime = Clock.now(),
    )

    private fun EmailSendingLog.Companion.ofFailed(email: Email, e: Exception) = ofFailed(
        id = ULID.random(),
        recipientAddress = email.recipient,
        senderAddress = senders[email.senderProfile],
        senderProfile = email.senderProfile.name,
        title = email.title,
        exception = e,
    )
}
