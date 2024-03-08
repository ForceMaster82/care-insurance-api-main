package kr.caredoc.careinsurance.aws.ses

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
import com.amazonaws.services.simpleemail.model.AmazonSimpleEmailServiceException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import kr.caredoc.careinsurance.email.Email
import kr.caredoc.careinsurance.email.EmailSendingLog
import kr.caredoc.careinsurance.email.EmailSendingLogRepository
import kr.caredoc.careinsurance.email.SenderProfile
import kr.caredoc.careinsurance.email.Senders
import kr.caredoc.careinsurance.relaxedMock

class SesEmailSenderTest : BehaviorSpec({
    given("SesEmailSender 가 주어졌을때") {
        val client = relaxedMock<AmazonSimpleEmailService>()
        val senders = Senders(
            infoProfileAddress = "info@caredoc.kr"
        )
        val emailSendingLogRepository = relaxedMock<EmailSendingLogRepository>()
        val emailSender = SesEmailSender(
            sesClient = client,
            senders = senders,
            emailSendingLogRepository = emailSendingLogRepository,
        )

        beforeEach {
            val entitySlot = slot<EmailSendingLog>()
            every { emailSendingLogRepository.save(capture(entitySlot)) } answers { entitySlot.captured }
        }

        afterEach { clearAllMocks() }

        `when`("이메일을 보내면") {
            val email = Email(
                title = "케어 인슈어런스 계정이 생성되었습니다.",
                content = "생성된 비밀번호는 아래와 같습니다.\n12345678",
                recipient = "recipient@caredoc.kr",
                senderProfile = SenderProfile.INFO,
            )

            fun behavior() = emailSender.send(email)

            then("주어진 이메일 제목과 같은 제목을 가진 이메일을 amazon ses 로 발송합니다.") {
                behavior()

                verify {
                    client.sendEmail(
                        withArg {
                            it.message.subject.data shouldBe "케어 인슈어런스 계정이 생성되었습니다."
                        }
                    )
                }
            }

            then("주어진 이메일 내용과 같은 내용을 가진 이메일을 amazon ses 로 발송합니다.") {
                behavior()

                verify {
                    client.sendEmail(
                        withArg {
                            it.message.body.text.data shouldBe "생성된 비밀번호는 아래와 같습니다.\n12345678"
                        }
                    )
                }
            }

            then("주어진 이메일의 수신인에게 이메일을 amazon ses 로 발송합니다.") {
                behavior()

                verify {
                    client.sendEmail(
                        withArg {
                            it.destination.toAddresses.size shouldBe 1
                            it.destination.toAddresses[0] shouldBe "recipient@caredoc.kr"
                        }
                    )
                }
            }

            then("주어진 이메일 발신 프로필에 입력된 이메일 주소를 발신인으로 하여 amazon ses로 발송합니다.") {
                behavior()

                verify {
                    client.sendEmail(
                        withArg {
                            it.source shouldBe "info@caredoc.kr"
                        }
                    )
                }
            }

            then("이메일 발송 성공 기록을 저장합니다.") {
                behavior()

                verify {
                    emailSendingLogRepository.save(
                        withArg {
                            it.recipientAddress shouldBe "recipient@caredoc.kr"
                            it.senderAddress shouldBe "info@caredoc.kr"
                            it.senderProfile shouldBe "INFO"
                            it.title shouldBe "케어 인슈어런스 계정이 생성되었습니다."
                            it.result shouldBe EmailSendingLog.SendingResult.SENT
                            it.sentDateTime shouldNotBe null
                        }
                    )
                }
            }

            and("하지만 이메일 발송에 실패했다면") {
                val occurredException = AmazonSimpleEmailServiceException("이메일 발송에 실패함")
                beforeEach {
                    every { client.sendEmail(any()) } throws occurredException
                }

                afterEach { clearAllMocks() }

                then("이메일 발송 실패 기록을 저장합니다.") {
                    shouldThrowAny { behavior() }

                    verify {
                        emailSendingLogRepository.save(
                            withArg {
                                it.recipientAddress shouldBe "recipient@caredoc.kr"
                                it.senderAddress shouldBe "info@caredoc.kr"
                                it.senderProfile shouldBe "INFO"
                                it.title shouldBe "케어 인슈어런스 계정이 생성되었습니다."
                                it.result shouldBe EmailSendingLog.SendingResult.FAILED
                                it.sentDateTime shouldBe null
                                it.reasonForFailure?.reasonForFailure shouldBe "com.amazonaws.services.simpleemail.model.AmazonSimpleEmailServiceException"
                                it.reasonForFailure?.failureMessage shouldBe "이메일 발송에 실패함 (Service: null; Status Code: 0; Error Code: null; Request ID: null; Proxy: null)"
                            }
                        )
                    }
                }

                then("발생한 예외를 전파합니다.") {
                    val thrownException = shouldThrow<AmazonSimpleEmailServiceException> { behavior() }

                    thrownException shouldBeSameInstanceAs occurredException
                }
            }
        }
    }
})
