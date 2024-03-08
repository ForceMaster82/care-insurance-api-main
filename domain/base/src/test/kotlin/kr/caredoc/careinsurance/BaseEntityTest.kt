package kr.caredoc.careinsurance

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

private class TestingPurposeEntity(id: String) : BaseEntity(id) {
    fun simulatePostLoad() = this.postLoad()
    fun simulatePostPersist() = this.postPersist()
}

class BaseEntityTest : BehaviorSpec({
    given("entity inherit base entity") {
        lateinit var entity: TestingPurposeEntity

        beforeEach {
            entity = TestingPurposeEntity("01GNTVARWHHG25ZND4WJBY28YG")
        }

        `when`("created") {
            then("isNew property should be true") {
                entity.isNew shouldBe true
            }
        }

        `when`("after loaded") {
            beforeEach {
                entity.simulatePostLoad()
            }

            then("isNew property should be false") {
                entity.isNew shouldBe false
            }
        }

        `when`("after persist") {
            beforeEach {
                entity.simulatePostPersist()
            }

            then("isNew property should be false") {
                entity.isNew shouldBe false
            }
        }
    }
})
