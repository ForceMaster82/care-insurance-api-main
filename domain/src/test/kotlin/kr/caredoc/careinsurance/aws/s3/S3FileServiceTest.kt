package kr.caredoc.careinsurance.aws.s3

import com.github.guepardoapps.kulid.ULID
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.mpp.file
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import kr.caredoc.careinsurance.file.FileByUrlQuery
import kr.caredoc.careinsurance.file.FileMeta
import kr.caredoc.careinsurance.file.FileMetaRepository
import kr.caredoc.careinsurance.file.FileSavingCommand
import kr.caredoc.careinsurance.file.GeneratingOpenedFileUrlCommand
import kr.caredoc.careinsurance.relaxedMock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ContentDisposition
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetUrlRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.io.ByteArrayInputStream
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
@SpringBootTest
class S3FileServiceTest(
    @Autowired
    private val cacheFileMetaRepository: FileMetaRepository,
) : BehaviorSpec({
    given("file service") {
        val amazonS3: S3Client = mockk(relaxed = true)
        val s3PreSigner = relaxedMock<S3Presigner>()
        val fileMetaRepository: FileMetaRepository = mockk(relaxed = true)
        val fileService = S3FileService(
            amazonS3 = amazonS3,
            s3PreSigner = s3PreSigner,
            fileMetaRepository = fileMetaRepository
        )

        beforeEach {
            val savingFileMetaSlot = slot<FileMeta>()
            every { fileMetaRepository.save(capture(savingFileMetaSlot)) } answers {
                savingFileMetaSlot.captured
            }
        }

        afterEach { clearAllMocks() }

        `when`("business license file can be uploaded") {
            val savedFileUrl =
                "https://careinsurance-business-license-dev.s3.ap-northeast-2.amazonaws.com/01GQ6QCW4A96Z53SAXKGRBN12F"

            beforeEach {
                mockkObject(ULID)
                every { ULID.random() } returns "01GQ4YX74B0PSE0BEPZKB0032H"

                every {
                    amazonS3.utilities()
                        .getUrl(any<GetUrlRequest>())
                } returns URL(savedFileUrl)
            }

            afterEach {
                clearAllMocks()
            }

            `when`("saving file") {
                val file = object {}.javaClass.getResourceAsStream("/business licenses/케어닥 사업자등록증.pdf")!!.readBytes()
                val fileStream = ByteArrayInputStream(file)
                val command = FileSavingCommand(
                    bucketName = "careinsurance-business-license-dev",
                    fileStream = fileStream,
                    contentLength = file.size.toLong(),
                    mime = "application/pdf",
                    path = "01GQ6QCW4A96Z53SAXKGRBN12F",
                )

                fun behavior() = fileService.saveFile(command)

                then("file meta entity should be persisted") {
                    behavior()

                    verify {
                        fileMetaRepository.save(
                            withArg {
                                it.status shouldBe FileMeta.Status.UPLOADED
                                it.bucket shouldBe "careinsurance-business-license-dev"
                                it.contentLength shouldBe file.size.toLong()
                                it.mime shouldBe "application/pdf"
                                it.path shouldBe "01GQ6QCW4A96Z53SAXKGRBN12F"
                            }
                        )
                    }
                }

                then("file should be uploaded to amazon s3") {
                    behavior()
                    verify {
                        amazonS3.putObject(
                            withArg<PutObjectRequest> {
                                it.bucket() shouldBe command.bucketName
                                it.contentLength() shouldBe command.contentLength
                                it.contentType() shouldBe command.mime
                                it.key() shouldBe command.path
                            },
                            any<RequestBody>(),
                        )
                    }
                }

                then("should returns uploaded resource url") {
                    val actualResult = behavior()

                    actualResult.savedFileUrl shouldBe savedFileUrl
                }
            }
        }

        `when`("business license file exists") {
            val fileMeta: FileMeta = mockk(relaxed = true)
            val fileUrl =
                "https://careinsurance-business-license-dev.s3.ap-northeast-2.amazonaws.com/01GQ6QCW4A96Z53SAXKGRBN12F"

            beforeEach {
                every { fileMeta.id } returns "01GPMV0JDMGPCE6TKARA2F7SVX"
                every { fileMeta.bucket } returns "careinsurance-business-license-dev"
                every { fileMeta.path } returns "01GQ6QCW4A96Z53SAXKGRBN12F"
                every { fileMeta.status } returns FileMeta.Status.UPLOADED
                every { fileMeta.url } returns fileUrl
                every { fileMeta.mime } returns "application/pdf"
                every { fileMeta.contentLength } returns 1L

                every {
                    fileMetaRepository.findByUrlAndStatus(fileUrl, FileMeta.Status.UPLOADED)
                } returns fileMeta

                every {
                    s3PreSigner.presignGetObject(
                        match<GetObjectPresignRequest> {
                            it.objectRequest.bucket() == "careinsurance-business-license-dev" &&
                                it.objectRequest.key() == "01GQ6QCW4A96Z53SAXKGRBN12F"
                        }
                    )
                } returns relaxedMock {
                    every { url().toString() } returns "https://careinsurance-business-license-dev.s3.ap-northeast-2.amazonaws.com/01GXQQJ25FC0230CNHTAGE08HA?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20230412T071407Z&X-Amz-SignedHeaders=host&X-Amz-Expires=28&X-Amz-Credential=AKIAUU4TUXH5YO4MV7VI%2F20230412%2Fap-northeast-2%2Fs3%2Faws4_request&X-Amz-Signature=0ed7ee3215a509415986436618804294ffbf78b488357e9037fc637b03b6936d"
                    every { expiration() } returns Instant.parse("2023-04-12T16:21:10+09:00")
                }
            }

            afterEach { clearAllMocks() }

            `when`("deleting file") {
                val query = FileByUrlQuery(
                    url = fileUrl,
                )

                fun behavior() = fileService.deleteFile(query)

                then("file meta should be marked as deleted") {
                    behavior()
                    verify { fileMeta.scheduleDelete() }
                }
            }

            `when`("등록된 파일의 개방된 URL을 요청하면") {
                val query = FileByUrlQuery(
                    url = "https://careinsurance-business-license-dev.s3.ap-northeast-2.amazonaws.com/01GQ6QCW4A96Z53SAXKGRBN12F",
                )
                val command = GeneratingOpenedFileUrlCommand(
                    duration = Duration.ofSeconds(30),
                )

                fun behavior() = fileService.generateOpenedUrl(query, command)

                then("url을 사용하여 업로드된 파일 메타데이터를 조회합니다.") {
                    behavior()

                    verify {
                        fileMetaRepository.findByUrlAndStatus(
                            "https://careinsurance-business-license-dev.s3.ap-northeast-2.amazonaws.com/01GQ6QCW4A96Z53SAXKGRBN12F",
                            FileMeta.Status.UPLOADED,
                        )
                    }
                }

                then("amazon s3에 presigned URL을 요청합니다.") {
                    behavior()

                    verify {
                        s3PreSigner.presignGetObject(
                            withArg<GetObjectPresignRequest> {
                                it.objectRequest.bucket() shouldBe "careinsurance-business-license-dev"
                                it.objectRequest.key() shouldBe "01GQ6QCW4A96Z53SAXKGRBN12F"
                            }
                        )
                    }
                }

                then("presigned url과 만료시간을 반환합니다.") {
                    val actualResult = behavior()

                    actualResult?.url shouldBe "https://careinsurance-business-license-dev.s3.ap-northeast-2.amazonaws.com/01GXQQJ25FC0230CNHTAGE08HA?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20230412T071407Z&X-Amz-SignedHeaders=host&X-Amz-Expires=28&X-Amz-Credential=AKIAUU4TUXH5YO4MV7VI%2F20230412%2Fap-northeast-2%2Fs3%2Faws4_request&X-Amz-Signature=0ed7ee3215a509415986436618804294ffbf78b488357e9037fc637b03b6936d"
                    actualResult?.expiration shouldBe LocalDateTime.of(2023, 4, 12, 16, 21, 10)
                }
            }

            `when`("개방된 URL을 Content-Disposition 옵션을 적용하여 요청하면") {
                val query = FileByUrlQuery(
                    url = "https://careinsurance-business-license-dev.s3.ap-northeast-2.amazonaws.com/01GQ6QCW4A96Z53SAXKGRBN12F",
                )
                val command = GeneratingOpenedFileUrlCommand(
                    duration = Duration.ofSeconds(30),
                    contentDisposition = ContentDisposition.attachment()
                        .filename("business license.pdf")
                        .build()
                )

                fun behavior() = fileService.generateOpenedUrl(query, command)

                then("amazon s3에 presigned URL을 요청합니다.") {
                    behavior()

                    verify {
                        s3PreSigner.presignGetObject(
                            withArg<GetObjectPresignRequest> {
                                it.objectRequest.bucket() shouldBe "careinsurance-business-license-dev"
                                it.objectRequest.key() shouldBe "01GQ6QCW4A96Z53SAXKGRBN12F"
                                it.objectRequest.responseContentDisposition() shouldBe "attachment; filename=\"business license.pdf\""
                            }
                        )
                    }
                }
            }
        }

        and("business license file not exists") {
            val fileUrl =
                "https://careinsurance-business-license-dev.s3.ap-northeast-2.amazonaws.com/01GPMV0JDMGPCE6TKARA2F7SVX"

            beforeEach {
                every {
                    fileMetaRepository.findByUrlAndStatus(fileUrl, FileMeta.Status.UPLOADED)
                } returns null
            }

            afterEach {
                clearAllMocks()
            }

            `when`("deleting file") {
                val query = FileByUrlQuery(
                    url = fileUrl,
                )

                fun behavior() = fileService.deleteFile(query)

                then("nothing happens") {
                    behavior()
                    shouldNotThrowAny {
                        fileService.deleteFile(query)
                    }
                }
            }

            `when`("개방된 URL을 요청하면") {
                val query = FileByUrlQuery(
                    url = "https://careinsurance-business-license-dev.s3.ap-northeast-2.amazonaws.com/01GPMV0JDMGPCE6TKARA2F7SVX",
                )
                val command = GeneratingOpenedFileUrlCommand(
                    duration = Duration.ofSeconds(30),
                )

                fun behavior() = fileService.generateOpenedUrl(query, command)

                then("url을 사용하여 업로드된 파일 메타데이터를 조회합니다.") {
                    behavior()

                    verify {
                        fileMetaRepository.findByUrlAndStatus(
                            "https://careinsurance-business-license-dev.s3.ap-northeast-2.amazonaws.com/01GPMV0JDMGPCE6TKARA2F7SVX",
                            FileMeta.Status.UPLOADED,
                        )
                    }
                }

                then("null을 반환합니다.") {
                    val actualResult = behavior()

                    actualResult shouldBe null
                }
            }
        }

        and("file meta exists but actual file not exists") {
            val fileUrl =
                "https://careinsurance-business-license-dev.s3.ap-northeast-2.amazonaws.com/01GQ6QCW4A96Z53SAXKGRBN12F"
            val fileMeta: FileMeta = mockk(relaxed = true)

            beforeEach {
                every {
                    fileMetaRepository.findByUrlAndStatus(fileUrl, FileMeta.Status.UPLOADED)
                } returns fileMeta

                with(fileMeta) {
                    every { bucket } returns "careinsurance-business-license-dev"
                    every { path } returns "01GQ6QCW4A96Z53SAXKGRBN12F"
                }

                val noSuchKeyException = NoSuchKeyException.builder().build()

                every {
                    amazonS3.deleteObject(any<DeleteObjectRequest>())
                } throws noSuchKeyException

                every {
                    s3PreSigner.presignGetObject(
                        match<GetObjectPresignRequest> {
                            it.objectRequest.bucket() == "careinsurance-business-license-dev" &&
                                it.objectRequest.key() == "01GQ6QCW4A96Z53SAXKGRBN12F"
                        }
                    )
                } throws noSuchKeyException
            }

            afterEach {
                clearAllMocks()
            }

            `when`("deleting file") {
                val query = FileByUrlQuery(
                    url = fileUrl,
                )

                fun behavior() = fileService.deleteFile(query)

                then("file meta should be marked as deleted") {
                    behavior()

                    verify { fileMeta.scheduleDelete() }
                }

                then("throw nothing") {
                    shouldNotThrowAny {
                        behavior()
                    }
                }
            }

            `when`("개방된 URL을 요청하면") {
                val query = FileByUrlQuery(
                    url = "https://careinsurance-business-license-dev.s3.ap-northeast-2.amazonaws.com/01GQ6QCW4A96Z53SAXKGRBN12F",
                )
                val command = GeneratingOpenedFileUrlCommand(
                    duration = Duration.ofSeconds(30),
                )

                fun behavior() = fileService.generateOpenedUrl(query, command)

                then("url을 사용하여 업로드된 파일 메타데이터를 조회합니다.") {
                    behavior()

                    verify {
                        fileMetaRepository.findByUrlAndStatus(
                            "https://careinsurance-business-license-dev.s3.ap-northeast-2.amazonaws.com/01GQ6QCW4A96Z53SAXKGRBN12F",
                            FileMeta.Status.UPLOADED,
                        )
                    }
                }

                then("amazon s3에 presigned URL을 요청합니다.") {
                    behavior()

                    verify {
                        s3PreSigner.presignGetObject(
                            withArg<GetObjectPresignRequest> {
                                it.objectRequest.bucket() shouldBe "careinsurance-business-license-dev"
                                it.objectRequest.key() shouldBe "01GQ6QCW4A96Z53SAXKGRBN12F"
                            }
                        )
                    }
                }

                then("null을 반환합니다.") {
                    val actualResult = behavior()

                    actualResult shouldBe null
                }
            }
        }

        and("엔티티 테스트 할 때") {
            val path = "01HFNBB5ESSNSZWXK9GS06FFXZ"
            val fileMeta = FileMeta(
                id = "01HFNB2RAG5Q561QGYVFDA50TD",
                bucket = "careinsurance-business-license-dev",
                path,
            )
            val url = "https://careinsurance-business-license-dev.s3.ap-northeast-2.amazonaws.com/$path"
            val mime = "application/pdf"
            val contentLength = 1L
            fileMeta.markAsUploaded(url, mime, contentLength)
            `when`("저장을 요청하면") {
                fun behavior() = cacheFileMetaRepository.save(fileMeta)
                then("저장이 됩니다.") {
                    behavior()
                }
            }
            `when`("조회를 요청하면") {
                fun behavior() = cacheFileMetaRepository.findByIdOrNull("01HFNB2RAG5Q561QGYVFDA50TD")
                then("조회가 됩니다.") {
                    behavior()
                }
            }
        }
    }
})
