package kr.caredoc.careinsurance.security.accesscontrol

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class CombinedSubjectTest : BehaviorSpec({
    given("subjects and its combined subject") {
        val subject1 = object : Subject {
            override fun get(attribute: SubjectAttribute): Set<String> {
                if (attribute == SubjectAttribute.USER_ID) {
                    return setOf("1")
                }
                if (attribute == SubjectAttribute.USER_TYPE) {
                    return setOf("1")
                }
                return setOf()
            }
        }

        val subject2 = object : Subject {
            override fun get(attribute: SubjectAttribute): Set<String> {
                if (attribute == SubjectAttribute.USER_TYPE) {
                    return setOf("1", "2")
                }
                return setOf()
            }
        }

        val combinedSubject = CombinedSubject(listOf(subject1, subject2))

        `when`("getting attributes") {
            then("returns attributes of combined") {
                combinedSubject[SubjectAttribute.USER_ID] shouldBe setOf("1")
                combinedSubject[SubjectAttribute.USER_TYPE] shouldBe setOf("1", "2")
            }
        }
    }
})
