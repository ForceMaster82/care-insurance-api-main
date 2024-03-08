package kr.caredoc.careinsurance.agency

import com.github.guepardoapps.kulid.ULID
import kr.caredoc.careinsurance.account.AccountInfo
import kr.caredoc.careinsurance.file.FileByUrlQuery
import kr.caredoc.careinsurance.file.FileByUrlQueryHandler
import kr.caredoc.careinsurance.file.FileSavingCommand
import kr.caredoc.careinsurance.file.FileSavingCommandHandler
import kr.caredoc.careinsurance.security.accesscontrol.Object
import kr.caredoc.careinsurance.security.accesscontrol.ReadOneAccess
import kr.caredoc.careinsurance.security.accesscontrol.checkAll
import kr.caredoc.careinsurance.withSort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ExternalCaregivingOrganizationService(
    private val fileSavingCommandHandler: FileSavingCommandHandler,
    private val fileByUrlQueryHandler: FileByUrlQueryHandler,
    private val externalCaregivingOrganizationRepository: ExternalCaregivingOrganizationRepository,
    @Value("\${cloud.aws.s3.bucket.careinsurance-business-license}")
    private val businessLicenseBucket: String
) : ExternalCaregivingOrganizationCreationCommandHandler,
    ExternalCaregivingOrganizationByIdQueryHandler,
    BusinessLicenseSavingCommandHandler,
    ExternalCaregivingOrganizationsByFilterQueryHandler,
    ExternalCaregivingOrganizationEditingCommandHandler,
    ExternalCaregivingOrganizationsByIdsQueryHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun createExternalCaregivingOrganization(command: ExternalCaregivingOrganizationCreationCommand): ExternalCaregivingOrganizationCreationResult {
        ExternalCaregivingOrganizationAccessPolicy.check(command.subject, command, Object.Empty)
        val newExternalCaregivingOrganization = command.intoEntity()
        externalCaregivingOrganizationRepository.save(newExternalCaregivingOrganization)

        return ExternalCaregivingOrganizationCreationResult(newExternalCaregivingOrganization.id)
    }

    private fun ExternalCaregivingOrganizationCreationCommand.intoEntity() = ExternalCaregivingOrganization(
        id = ULID.random(),
        name = this.name,
        externalCaregivingOrganizationType = this.externalCaregivingOrganizationType,
        address = this.address,
        contractName = this.contractName,
        phoneNumber = this.phoneNumber,
        profitAllocationRatio = this.profitAllocationRatio,
        accountInfo = AccountInfo(
            bank = this.accountInfo.bank,
            accountNumber = this.accountInfo.accountNumber,
            accountHolder = this.accountInfo.accountHolder,
        ),
    )

    @Transactional(readOnly = true)
    override fun getExternalCaregivingOrganization(query: ExternalCaregivingOrganizationByIdQuery): ExternalCaregivingOrganization {
        val externalCaregivingOrganization = externalCaregivingOrganizationRepository.findByIdOrNull(query.id)
            ?: throw ExternalCaregivingOrganizationNotFoundByIdException(query.id)

        ExternalCaregivingOrganizationAccessPolicy.check(query.subject, query, externalCaregivingOrganization)

        return externalCaregivingOrganization
    }

    @Transactional(readOnly = true)
    override fun ensureExternalCaregivingOrganizationExists(query: ExternalCaregivingOrganizationByIdQuery) {
        ExternalCaregivingOrganizationAccessPolicy.check(query.subject, query, Object.Empty)
        if (!externalCaregivingOrganizationRepository.existsById(query.id)) {
            throw ExternalCaregivingOrganizationNotFoundByIdException(query.id)
        }
    }

    @Transactional
    override fun saveBusinessLicenseFile(command: BusinessLicenseSavingCommand): BusinessLicenseSavingResult =
        run {
            ensureExternalCaregivingOrganizationExists(
                ExternalCaregivingOrganizationByIdQuery(
                    command.externalCaregivingOrganizationId,
                    command.subject
                )
            )

            fileSavingCommandHandler.saveFile(
                FileSavingCommand(
                    path = ULID.random(),
                    bucketName = businessLicenseBucket,
                    fileStream = command.businessLicenseFile,
                    mime = command.mime,
                    contentLength = command.contentLength,
                )
            )
        }.also { fileSavingResult ->
            runCatching {
                val externalCaregivingOrganization =
                    getExternalCaregivingOrganization(
                        ExternalCaregivingOrganizationByIdQuery(
                            id = command.externalCaregivingOrganizationId,
                            subject = command.subject,
                        )
                    )

                externalCaregivingOrganization.updateBusinessLicenseInfo(
                    SavedBusinessLicenseFileData(
                        command.businessLicenseFileName,
                        fileSavingResult.savedFileUrl
                    )
                )

                ExternalCaregivingOrganizationAccessPolicy.check(command.subject, command, Object.Empty)

                externalCaregivingOrganizationRepository.save(externalCaregivingOrganization)
            }.onFailure {
                deleteBusinessLicenseFile(fileSavingResult.savedFileUrl)
            }
        }.let { fileSavingResult ->
            BusinessLicenseSavingResult(
                fileSavingResult.savedFileUrl,
            )
        }

    private fun deleteBusinessLicenseFile(savedFileUrl: String) = runCatching {
        fileByUrlQueryHandler.deleteFile(FileByUrlQuery(savedFileUrl))
    }.onFailure { handleBusinessLicenseFileDeletionFailed(it) }

    private fun handleBusinessLicenseFileDeletionFailed(e: Throwable) {
        logger.warn("failed to delete external caregiving organization's business license file", e)
    }

    @Transactional(readOnly = true)
    override fun getExternalCaregivingOrganizations(
        query: ExternalCaregivingOrganizationsByFilterQuery,
        pageRequest: Pageable,
    ): Page<ExternalCaregivingOrganization> {
        //ExternalCaregivingOrganizationAccessPolicy.check(query.subject, query, Object.Empty)

        val nameQuery = query.getKeyword(
            propertyToExtractionKeyword = ExternalCaregivingOrganizationsByFilterQuery.SearchingProperty.EXTERNAL_CAREGIVING_ORGANIZATION_NAME
        )

        val sort = Sort.by(Sort.Order.desc("id"))

        return if (nameQuery != null && query.organizationType != null) {
            externalCaregivingOrganizationRepository.findByExternalCaregivingOrganizationTypeAndNameContains(
                query.organizationType,
                nameQuery,
                pageRequest.withSort(sort),
            )
        } else if (nameQuery != null) {
            externalCaregivingOrganizationRepository.findByNameContains(nameQuery, pageRequest.withSort(sort))
        } else if (query.organizationType != null) {
            externalCaregivingOrganizationRepository.findByExternalCaregivingOrganizationType(
                query.organizationType,
                pageRequest.withSort(sort),
            )
        } else {
            externalCaregivingOrganizationRepository.findAll(pageRequest.withSort(sort))
        }
    }

    private fun ExternalCaregivingOrganizationsByFilterQuery.getKeyword(
        propertyToExtractionKeyword: ExternalCaregivingOrganizationsByFilterQuery.SearchingProperty
    ) = if (searchCondition?.searchingProperty == propertyToExtractionKeyword) {
        searchCondition.keyword
    } else {
        null
    }

    @Transactional
    override fun editExternalCaregivingOrganization(command: ExternalCaregivingOrganizationEditingCommand) {
        getExternalCaregivingOrganization(
            ExternalCaregivingOrganizationByIdQuery(command.externalCaregivingOrganizationId, command.subject)
        ).editMetaData(command)
    }

    @Transactional
    override fun getExternalCaregivingOrganizations(query: ExternalCaregivingOrganizationsByIdsQuery): List<ExternalCaregivingOrganization> {
        ExternalCaregivingOrganizationAccessPolicy.check(query.subject, query, Object.Empty)
        val caregivingOrganizations = externalCaregivingOrganizationRepository.findByIdIn(
            query.externalCaregivingOrganizationIds,
        )

        ExternalCaregivingOrganizationAccessPolicy.checkAll(query.subject, ReadOneAccess, caregivingOrganizations)

        return caregivingOrganizations
    }
}
