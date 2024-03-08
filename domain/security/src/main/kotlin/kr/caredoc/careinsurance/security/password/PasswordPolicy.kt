package kr.caredoc.careinsurance.security.password

import kotlin.random.Random
import kotlin.random.nextInt

object PasswordPolicy {
    private val lengthPolicy = LengthPolicy(
        minLength = 8,
        maxLength = 20,
        recommendedMinLength = 14,
    )
    private val shouldContainingPolicies = listOf(
        CharacterContainingPolicy("abcdefghijklmnopqrstuvwxyz"),
        CharacterContainingPolicy("ABCDEFGHIJKLMNOPQRSTUVWXYZ"),
        CharacterContainingPolicy("0123456789"),
        CharacterContainingPolicy("!@#$%^&*-_"),
    )
    private val shouldNotContainingPolicies = listOf(
        CharacterNotContainingPolicy(" ")
    )
    private val asciiOnlyCharacterPolicy = AsciiOnlyCharacterPolicy()

    fun isLegalPassword(rawPassword: String): Boolean {
        return lengthPolicy.verifyLength(rawPassword) &&
            shouldContainingPolicies.all { it.verifyContains(rawPassword) } &&
            shouldNotContainingPolicies.all { it.verifyNotContains(rawPassword) } &&
            asciiOnlyCharacterPolicy.verifyNotContains(rawPassword)
    }

    fun ensurePasswordLegal(rawPassword: String) {
        if (!isLegalPassword(rawPassword)) {
            throw IllegalPasswordException()
        }
    }

    private const val MINIMUM_CHAR_COUNT_FOR_CATEGORY = 3

    fun generateRandomPassword(): String {
        val length = lengthPolicy.generateValidLength()

        // 필수 구성요소를 각각 최소 구성개수 만큼 + 남는 길이만큼의 무작위 구성요소들
        val categories = shouldContainingPolicies.indices.asSequence().flatMap {
            generateSequence { it }.take(MINIMUM_CHAR_COUNT_FOR_CATEGORY)
        } + generateSequence { Random.nextInt(shouldContainingPolicies.indices) }.take(length - (MINIMUM_CHAR_COUNT_FOR_CATEGORY * shouldContainingPolicies.size))

        return categories.map { shouldContainingPolicies[it].generateRandomValidCharacter() }.shuffled()
            .joinToString("")
    }

    private class LengthPolicy(
        private val minLength: Int,
        private val maxLength: Int,
        private val recommendedMinLength: Int,
    ) {
        fun verifyLength(target: String): Boolean {
            return target.length in minLength..maxLength
        }

        fun generateValidLength(): Int = Random.nextInt(recommendedMinLength..maxLength)
    }

    private class CharacterContainingPolicy(private val chars: String) {
        fun verifyContains(target: String): Boolean {
            val policyChars = chars.toCharArray().toSet()
            val targetChars = target.toCharArray().toSet()

            return (policyChars intersect targetChars).isNotEmpty()
        }

        fun generateRandomValidCharacter(): Char {
            return chars[Random.nextInt(chars.length)]
        }
    }

    private class CharacterNotContainingPolicy(private val chars: String) {
        fun verifyNotContains(target: String): Boolean {
            val policyChars = chars.toCharArray().toSet()
            val targetChars = target.toCharArray().toSet()

            return (policyChars intersect targetChars).isEmpty()
        }
    }

    private class AsciiOnlyCharacterPolicy {
        fun verifyNotContains(target: String): Boolean {
            return target.chars().allMatch {
                // 0부터 33, 127은 제어문자입니다.
                it in 33..126
            }
        }
    }
}
