package kr.caredoc.careinsurance.user

import kr.caredoc.careinsurance.email.Email
import kr.caredoc.careinsurance.email.SenderProfile
import org.springframework.stereotype.Component

@Component
class NewUserPasswordGuidanceEmailTemplate {
    data class TemplateData(
        val userEmail: String,
        val userName: String,
        val rawPassword: String,
    )

    fun generate(data: TemplateData): Email {
        return Email(
            title = "[케어 인슈어런스] 가입 및 로그인 안내",
            content = generateContent(data),
            recipient = data.userEmail,
            senderProfile = SenderProfile.INFO,
        )
    }

    private fun generateContent(data: TemplateData) =
        """
            안녕하세요,

            케어 인슈어런스에 가입해 주셔서 감사합니다. 사용자 계정이 성공적으로 생성되었으며, 이메일을 통해 로그인하실 수 있습니다.

            아래는 현재 사용자 계정에 대한 정보입니다:

            이메일: ${data.userEmail}
            현재 비밀번호: ${data.rawPassword}

            임시 비밀번호는 보안을 위해 무작위로 생성되었습니다. 로그인 후에는 언제든지 비밀번호를 수정하실 수 있습니다.

            더 자세한 내용이나 도움이 필요하시면 케어닥 제휴운영팀(1833-2799)으로 문의주시기 바랍니다. 

            감사합니다.

            케어 인슈어런스
        """.trimIndent()
}
