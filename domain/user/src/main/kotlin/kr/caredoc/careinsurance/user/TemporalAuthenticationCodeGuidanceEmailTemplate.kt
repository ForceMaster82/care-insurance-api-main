package kr.caredoc.careinsurance.user

import kr.caredoc.careinsurance.email.Email
import kr.caredoc.careinsurance.email.SenderProfile
import org.springframework.stereotype.Component

@Component
class TemporalAuthenticationCodeGuidanceEmailTemplate {
    data class TemplateData(
        val userEmail: String,
        val userName: String,
        val authenticationCode: String,
    )

    fun generate(data: TemplateData): Email {
        return Email(
            title = "[케어 인슈어런스] 인증번호 안내",
            content = generateContent(data),
            recipient = data.userEmail,
            senderProfile = SenderProfile.INFO,
        )
    }

    private fun generateContent(data: TemplateData) =
        """
            안녕하세요,

            케어 인슈어런스에서는 사용자의 요청을 받아 임시 인증번호가 생성되었음을 알려드립니다. 이 인증번호를 입력하시면 일시적으로 서비스에 접근하실 수 있습니다.

            아래는 임시 인증번호입니다:

            인증번호: ${data.authenticationCode}

            더 자세한 내용이나 도움이 필요하시면 케어닥 제휴운영팀(1833-2799)으로 문의주시기 바랍니다. 

            감사합니다.

            케어 인슈어런스
        """.trimIndent()
}
