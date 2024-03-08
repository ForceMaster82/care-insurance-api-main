package kr.caredoc.careinsurance.security.hash

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.random.Random

class PepperedHasherTest : BehaviorSpec({
    given("해셔가 주어졌을때") {
        val pepper = Random.nextBytes(32)
        val hasher = PepperedHasher(pepper)

        `when`("동일한 평문을 여러차례 해싱하면") {
            val plain = Random.nextBytes(128)

            val hashOne = hasher.hash(plain)
            val hashAnother = hasher.hash(plain)

            then("생성된 해시값은 동일해야 합니다.") {
                hashOne shouldBe hashAnother
            }
        }

        `when`("동일하지 않은 평문들을 해싱하면") {
            val plainOne = Random.nextBytes(128)
            var plainAnother = Random.nextBytes(128)
            while (plainOne contentEquals plainAnother) {
                plainAnother = Random.nextBytes(128)
            }

            val hashOne = hasher.hash(plainOne)
            val hashAnother = hasher.hash(plainAnother)

            then("생성된 해시값은 달라야 합니다.") {
                hashOne shouldNotBe hashAnother
            }
        }

        and("동일한 페퍼를 가진 해셔가 하나 더 주어졌을때") {
            val anotherHasher = PepperedHasher(pepper)

            `when`("동일한 평문을 각각의 해셔로 해싱하면") {
                val plain = Random.nextBytes(128)

                val hashOne = hasher.hash(plain)
                val hashAnother = anotherHasher.hash(plain)

                then("생성된 해시값은 동일해야 합니다.") {
                    hashOne shouldBe hashAnother
                }
            }
        }

        and("다른 페퍼를 가진 해셔가 하나 더 주어졌을때 ") {
            val anotherPepper = Random.nextBytes(32)
            var anotherHasher = PepperedHasher(anotherPepper)
            while (anotherPepper contentEquals pepper) {
                anotherHasher = PepperedHasher(anotherPepper)
            }

            `when`("동일한 평문을 각각의 해셔로 해싱하면") {
                val plain = Random.nextBytes(128)

                val hashOne = hasher.hash(plain)
                val hashAnother = anotherHasher.hash(plain)

                then("생성된 해시값은 달라야 합니다.") {
                    hashOne shouldNotBe hashAnother
                }
            }
        }
    }
})
