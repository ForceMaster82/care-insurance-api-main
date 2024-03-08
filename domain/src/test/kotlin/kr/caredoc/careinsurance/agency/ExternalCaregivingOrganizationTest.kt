package kr.caredoc.careinsurance.agency

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kr.caredoc.careinsurance.account.AccountInfo
import kr.caredoc.careinsurance.generateInternalCaregivingManagerSubject
import kr.caredoc.careinsurance.security.accesscontrol.ObjectAttribute

class ExternalCaregivingOrganizationTest : BehaviorSpec({
    given("external caregiving organization") {
        val path = "01GQP0PQCA68T6CMKV7AV0TPVV"
        val accountEntity = AccountInfo(
            bank = "국민은행",
            accountNumber = "085300-04-111424",
            accountHolder = "박한결",
        )
        val externalCaregivingOrganization = ExternalCaregivingOrganization(
            id = "01GQNZQ5V3CZBXJ6B95JJRYMWN",
            name = "케어라인",
            externalCaregivingOrganizationType = ExternalCaregivingOrganizationType.AFFILIATED,
            address = "서울시 강남구 삼성동 109-1",
            contractName = "김라인",
            phoneNumber = "010-1234-1234",
            profitAllocationRatio = 0.0f,
            accountInfo = accountEntity,
        )

        `when`("updating business license info") {
            val name = "케어라인 사업자등록증.pdf"
            val url = "https://careinsurance-business-license-dev.s3.ap-northeast-2.amazonaws.com/$path"

            fun behavior() = externalCaregivingOrganization.updateBusinessLicenseInfo(
                SavedBusinessLicenseFileData(
                    name,
                    url,
                )
            )
            then(" fileName and url updated") {
                behavior()

                externalCaregivingOrganization.businessLicenseFileName shouldBe name
                externalCaregivingOrganization.businessLicenseFileUrl shouldBe url
            }
        }

        `when`("editing external caregiving organization meta data") {
            fun behavior() = externalCaregivingOrganization.editMetaData(
                ExternalCaregivingOrganizationEditingCommand(
                    name = "케어라인",
                    externalCaregivingOrganizationId = "01GQNZQ5V3CZBXJ6B95JJRYMWN",
                    externalCaregivingOrganizationType = ExternalCaregivingOrganizationType.AFFILIATED,
                    address = "서울시 강남구 삼성동 109-11",
                    contractName = "김케어",
                    phoneNumber = "010-1234-4444",
                    profitAllocationRatio = 0.0f,
                    accountInfo = AccountInfo(
                        bank = "신한은행",
                        accountNumber = "111-444-555888",
                        accountHolder = "박결한"
                    ),
                    subject = generateInternalCaregivingManagerSubject()
                )
            )

            then("external caregiving organization properties are should be changed") {
                behavior()

                externalCaregivingOrganization.name shouldBe "케어라인"
                externalCaregivingOrganization.externalCaregivingOrganizationType shouldBe ExternalCaregivingOrganizationType.AFFILIATED
                externalCaregivingOrganization.address shouldBe "서울시 강남구 삼성동 109-11"
                externalCaregivingOrganization.contractName shouldBe "김케어"
                externalCaregivingOrganization.phoneNumber shouldBe "010-1234-4444"
                externalCaregivingOrganization.profitAllocationRatio shouldBe 0.0f
                externalCaregivingOrganization.accountInfo?.bank shouldBe "신한은행"
                externalCaregivingOrganization.accountInfo?.accountNumber shouldBe "111-444-555888"
                externalCaregivingOrganization.accountInfo?.accountHolder shouldBe "박결한"
            }
        }

        `when`("ID 접근 대상 속성을 조회하면") {
            fun behavior() = externalCaregivingOrganization[ObjectAttribute.ID]

            then("간병 협회의 아이디를 포함한 셋을 반환합니다.") {
                val actualResult = behavior()

                actualResult shouldContain "01GQNZQ5V3CZBXJ6B95JJRYMWN"
            }
        }
    }
})
