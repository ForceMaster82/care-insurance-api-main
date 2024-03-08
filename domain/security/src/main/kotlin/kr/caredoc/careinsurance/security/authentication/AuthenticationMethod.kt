package kr.caredoc.careinsurance.security.authentication

enum class AuthenticationMethod {
    ID_PW_LOGIN,
    TEMPORAL_CODE;

    companion object {
        private val VALUES_BY_NAME = AuthenticationMethod.values().associateBy { it.name }

        private fun generateIllegalValueMessage(value: String) = "${value}는 AuthenticationMethod로 해석될 수 없습니다."

        fun parse(value: String): AuthenticationMethod {
            return VALUES_BY_NAME[value] ?: throw IllegalArgumentException(generateIllegalValueMessage(value))
        }

        fun ensureParsable(value: String) = require(VALUES_BY_NAME.contains(value)) {
            generateIllegalValueMessage(value)
        }
    }
}
