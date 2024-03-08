package kr.caredoc.careinsurance.file

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class FileMetaTest : BehaviorSpec({
    given("file meta") {
        val path = "01GQP1GN4EJA8B088XGSRRTRF4"
        val fileMeta = FileMeta(
            id = "01GQP1CJMHKV5B48ZHEWJ8R619",
            bucket = "careinsurance-business-license",
            path = path,
        )

        `when`("checking initial properties") {
            then("should be READY") {
                fileMeta.status shouldBe FileMeta.Status.READY
            }

            then("should be empty") {
                fileMeta.url shouldBe null
            }
        }

        `when`("marking as uploaded") {
            val url = "https://careinsurance-business-license-dev.s3.ap-northeast-2.amazonaws.com/$path"
            val mime = "application/pdf"
            val contentLength = 1L
            fun behavior() = fileMeta.markAsUploaded(url, mime, contentLength)

            then("status and url updated") {
                behavior()

                fileMeta.status shouldBe FileMeta.Status.UPLOADED
                fileMeta.url shouldBe url
                fileMeta.mime shouldBe mime
                fileMeta.contentLength shouldBe contentLength
            }
        }

        `when`("it marked as uploaded") {
            val url = "https://careinsurance-business-license-dev.s3.ap-northeast-2.amazonaws.com/$path"
            val mime = "application/pdf"
            val contentLength = 1L

            beforeEach {
                fileMeta.markAsUploaded(url, mime, contentLength)
            }

            `when`("삭제 예정상태로 전환하면") {
                fun behavior() = fileMeta.scheduleDelete()

                then("삭제 예정으로 상태를 갱신합니다.") {
                    behavior()
                    fileMeta.status shouldBe FileMeta.Status.TO_BE_DELETED
                }

                then("url remains") {
                    behavior()
                    fileMeta.url shouldBe url
                }

                `when`("삭제 상태로 전환하면") {
                    fun behavior() = fileMeta.markAsDeleted()

                    then("삭제 상태로 갱신합니다.") {
                        behavior()

                        fileMeta.status shouldBe FileMeta.Status.DELETED
                    }
                }
            }
        }
    }
})
