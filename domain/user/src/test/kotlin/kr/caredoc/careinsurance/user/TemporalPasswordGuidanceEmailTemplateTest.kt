package kr.caredoc.careinsurance.user

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kr.caredoc.careinsurance.email.SenderProfile

class TemporalPasswordGuidanceEmailTemplateTest : BehaviorSpec({
    given("템플릿과 데이터가 주어졌을때") {
        val template = TemporalPasswordGuidanceEmailTemplate()
        val data = TemporalPasswordGuidanceEmailTemplate.TemplateData(
            userEmail = "boris@caredoc.kr",
            userName = "보리스",
            temporalRawPassword = "1Q2w3e4r!!",
        )

        `when`("이메일을 생성하면") {
            fun behavior() = template.generate(data)

            then("생성된 이메일은 고정된 제목을 가진다.") {
                val generatedEmail = behavior()

                generatedEmail.title shouldBe "[케어 인슈어런스] 임시 비밀번호 발급 안내"
            }

            then("생성된 이메일의 본문은 데이터로부터 생성된다.") {
                val generatedEmail = behavior()

                generatedEmail.content shouldBe """
                    안녕하세요,

                    케어 인슈어런스에서 임시 비밀번호가 생성되었음을 알려드립니다. 임시 비밀번호로 로그인하여 계정에 접근하실 수 있습니다.

                    아래는 현재 사용자 계정에 대한 정보입니다:

                    이메일: boris@caredoc.kr
                    현재 비밀번호: 1Q2w3e4r!!

                    임시 비밀번호는 보안을 위해 무작위로 생성되었습니다. 로그인 후에는 언제든지 비밀번호를 수정하실 수 있습니다.

                    더 자세한 내용이나 도움이 필요하시면 케어닥 제휴운영팀(1833-2799)으로 문의주시기 바랍니다. 

                    감사합니다.

                    케어 인슈어런스
                """.trimIndent()
            }

            then("생성된 이메일의 받는 사람은 데이터에 명시된 받는 사람이다.") {
                val generatedEmail = behavior()

                generatedEmail.recipient shouldBe "boris@caredoc.kr"
            }

            then("생성된 이메일의 발신 프로필은 INFO를 사용한다.") {
                val generatedEmail = behavior()

                generatedEmail.senderProfile shouldBe SenderProfile.INFO
            }
        }

        `when`("또 다른 데이터로 이메일을 생성하면") {
            val anotherData = TemporalPasswordGuidanceEmailTemplate.TemplateData(
                userEmail = "leo@caredoc.kr",
                userName = "레오",
                temporalRawPassword = "4Q3w2e1r!!",
            )

            fun behavior() = template.generate(anotherData)

            then("생성된 이메일은 데이터에 의해 영향을 받는다.") {
                val generatedEmail = behavior()

                generatedEmail.content shouldBe """
                    안녕하세요,

                    케어 인슈어런스에서 임시 비밀번호가 생성되었음을 알려드립니다. 임시 비밀번호로 로그인하여 계정에 접근하실 수 있습니다.

                    아래는 현재 사용자 계정에 대한 정보입니다:

                    이메일: leo@caredoc.kr
                    현재 비밀번호: 4Q3w2e1r!!

                    임시 비밀번호는 보안을 위해 무작위로 생성되었습니다. 로그인 후에는 언제든지 비밀번호를 수정하실 수 있습니다.

                    더 자세한 내용이나 도움이 필요하시면 케어닥 제휴운영팀(1833-2799)으로 문의주시기 바랍니다. 

                    감사합니다.

                    케어 인슈어런스
                """.trimIndent()
                generatedEmail.recipient shouldBe "leo@caredoc.kr"
            }
        }
    }
})
