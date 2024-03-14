package kr.caredoc.careinsurance.web.reception

import kr.caredoc.careinsurance.atLocalTimeZone
import kr.caredoc.careinsurance.insurance.InsuranceInfo
import kr.caredoc.careinsurance.insurance.InsuranceManagerInfo
import kr.caredoc.careinsurance.intoUtcOffsetDateTime
import kr.caredoc.careinsurance.patient.EncryptedPatientInfo
import kr.caredoc.careinsurance.patient.PatientInfo
import kr.caredoc.careinsurance.reception.AccidentInfo
import kr.caredoc.careinsurance.reception.CaregivingManagerInfo
import kr.caredoc.careinsurance.reception.OrganizationType
import kr.caredoc.careinsurance.reception.Reception
import kr.caredoc.careinsurance.reception.ReceptionByIdQuery
import kr.caredoc.careinsurance.reception.ReceptionByIdQueryHandler
import kr.caredoc.careinsurance.reception.ReceptionCreationCommand
import kr.caredoc.careinsurance.reception.ReceptionCreationCommandHandler
import kr.caredoc.careinsurance.reception.ReceptionCreationResult
import kr.caredoc.careinsurance.reception.ReceptionEditingCommand
import kr.caredoc.careinsurance.reception.ReceptionEditingCommandHandler
import kr.caredoc.careinsurance.reception.ReceptionProgressingStatus
import kr.caredoc.careinsurance.reception.ReceptionsByFilterQuery
import kr.caredoc.careinsurance.reception.ReceptionsByFilterQueryHandler
import kr.caredoc.careinsurance.reception.ReferenceCoverageNotExistsException
import kr.caredoc.careinsurance.reception.RegisterManagerInfo
import kr.caredoc.careinsurance.reception.exception.InvalidReceptionProgressingStatusTransitionException
import kr.caredoc.careinsurance.security.accesscontrol.Subject
import kr.caredoc.careinsurance.security.encryption.Decryptor
import kr.caredoc.careinsurance.web.paging.PagingRequest
import kr.caredoc.careinsurance.web.paging.intoPageable
import kr.caredoc.careinsurance.web.paging.intoPagedResponse
import kr.caredoc.careinsurance.web.reception.request.ReceptionCreationRequest
import kr.caredoc.careinsurance.web.reception.request.ReceptionEditingRequest
import kr.caredoc.careinsurance.web.reception.request.UnmaskablePersonalProperty
import kr.caredoc.careinsurance.web.reception.response.DetailReceptionResponse
import kr.caredoc.careinsurance.web.reception.response.InvalidReceptionProgressingStatusEnteredData
import kr.caredoc.careinsurance.web.reception.response.NotExistingReferenceCoverageData
import kr.caredoc.careinsurance.web.reception.response.SimpleReceptionResponse
import kr.caredoc.careinsurance.web.request.DatePeriodSpecifyingRequest
import kr.caredoc.careinsurance.web.response.GeneralErrorResponse
import kr.caredoc.careinsurance.web.search.QueryParser
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
@RequestMapping("/api/v1/receptions")
class ReceptionController(
    private val receptionCreationCommandHandler: ReceptionCreationCommandHandler,
    private val receptionsByFilterQueryHandler: ReceptionsByFilterQueryHandler,
    private val receptionByIdQueryHandler: ReceptionByIdQueryHandler,
    private val receptionEditingCommandHandler: ReceptionEditingCommandHandler,
    private val decryptor: Decryptor,
) {
    private val queryParser = QueryParser(
        mapOf(
            "insuranceNumber" to ReceptionsByFilterQuery.SearchingProperty.INSURANCE_NUMBER,
            "patientName" to ReceptionsByFilterQuery.SearchingProperty.PATIENT_NAME,
            "caregivingManagerName" to ReceptionsByFilterQuery.SearchingProperty.CAREGIVING_MANAGER_NAME,
            "patientPhoneNumber" to ReceptionsByFilterQuery.SearchingProperty.PATIENT_PHONE_NUMBER,
            "accidentNumber" to ReceptionsByFilterQuery.SearchingProperty.ACCIDENT_NUMBER,
            "caregiverName" to ReceptionsByFilterQuery.SearchingProperty.CAREGIVER_NAME,
            "hospitalAndRoom" to ReceptionsByFilterQuery.SearchingProperty.HOSPITAL_AND_ROOM,
        )
    )

    @PostMapping
    fun createReception(
        @RequestBody payload: ReceptionCreationRequest,
        subject: Subject,
    ): ResponseEntity<Unit> =
        receptionCreationCommandHandler.createReception(payload.intoCommand(subject))
            .intoResponse()

    private fun ReceptionCreationResult.intoResponse() = ResponseEntity
        .created(this.intoLocationHeader())
        .build<Unit>()

    private fun ReceptionCreationResult.intoLocationHeader() = ServletUriComponentsBuilder.fromCurrentRequest()
        .replacePath("/api/v1/receptions/${this.createdReceptionId}")
        .build()
        .toUri()

    private fun ReceptionCreationRequest.intoCommand(subject: Subject) = ReceptionCreationCommand(
        insuranceInfo = this.insuranceInfo.intoCommandDataSet(),
        patientInfo = this.patientInfo.intoCommandDataSet(),
        accidentInfo = this.accidentInfo.intoCommandDataSet(),
        insuranceManagerInfo = this.insuranceManagerInfo.intoCommandDataSet(),
        registerManagerInfo = this.registerManagerInfo.intoCommandDataSet(),
        receivedDateTime = this.receivedDateTime.atLocalTimeZone(),
        desiredCaregivingStartDate = this.desiredCaregivingStartDate,
        urgency = this.urgency,
        desiredCaregivingPeriod = this.desiredCaregivingPeriod,
        additionalRequests = this.additionalRequests,
        notifyCaregivingProgress = notifyCaregivingProgress,
        subject = subject,
    )

    private fun ReceptionCreationRequest.InsuranceInfo.intoCommandDataSet() = InsuranceInfo(
        insuranceNumber = this.insuranceNumber,
        subscriptionDate = this.subscriptionDate,
        coverageId = this.coverageId,
        caregivingLimitPeriod = this.caregivingLimitPeriod,
    )

    private fun ReceptionCreationRequest.PatientInfo.intoCommandDataSet() = PatientInfo(
        name = this.name,
        nickname = this.nickname,
        age = this.age,
        sex = this.sex,
        weight = null,
        height = null,
        primaryContact = this.primaryContact.intoCommandDataSet(),
        secondaryContact = this.secondaryContact?.intoCommandDataSet(),
    )

    private fun ReceptionCreationRequest.PatientContact.intoCommandDataSet() = PatientInfo.Contact(
        phoneNumber = this.phoneNumber,
        relationshipWithPatient = this.relationshipWithPatient,
    )

    private fun ReceptionCreationRequest.RegisterManagerInfo.intoCommandDataSet() = RegisterManagerInfo(
        managingUserId = this.managingUserId,
    )

    private fun ReceptionCreationRequest.AccidentInfo.intoCommandDataSet() = AccidentInfo(
        accidentNumber = this.accidentNumber,
        accidentDateTime = this.accidentDateTime.atLocalTimeZone(),
        claimType = this.claimType,
        patientDescription = this.patientDescription,
        admissionDateTime = this.admissionDateTime.atLocalTimeZone(),
        hospitalAndRoomInfo = AccidentInfo.HospitalAndRoomInfo(
            state = null,
            city = null,
            hospitalAndRoom = this.hospitalRoomInfo.hospitalAndRoom,
        )
    )

    private fun ReceptionCreationRequest.InsuranceManagerInfo.intoCommandDataSet() = InsuranceManagerInfo(
        branchName = this.branchName,
        receptionistName = this.receptionistName,
        phoneNumber = this.phoneNumber,
    )

    @GetMapping
    fun getReceptions(
        pagingRequest: PagingRequest,
        datePeriodSpecifyingRequest: DatePeriodSpecifyingRequest,
        @RequestParam("urgency", required = false) urgency: Reception.Urgency?,
        @RequestParam("period-type", required = false) periodType: Reception.PeriodType?,
        @RequestParam(
            "caregiving-manager-assigned",
            required = false,
        ) caregivingManagerAssigned: Boolean?,
        @RequestParam(
            "organization-type",
            required = false
        ) organizationType: OrganizationType?,
        @RequestParam("progressing-status", defaultValue = "") progressingStatuses: Set<ReceptionProgressingStatus>,
        @RequestParam("query", required = false) query: String?,
        subject: Subject,
    ) = ResponseEntity.ok(
        receptionsByFilterQueryHandler.getReceptions(
            ReceptionsByFilterQuery(
                from = datePeriodSpecifyingRequest.from,
                until = datePeriodSpecifyingRequest.until,
                urgency = urgency,
                periodType = periodType,
                caregivingManagerAssigned = caregivingManagerAssigned,
                organizationType = organizationType,
                progressingStatuses = progressingStatuses,
                searchCondition = query?.let { queryParser.parse(it) },
                subject = subject,
            ),
            pageRequest = pagingRequest.intoPageable(),
        ).map {
            it.intoSimpleResponse()
        }.intoPagedResponse()
    )

    fun Reception.intoSimpleResponse() = SimpleReceptionResponse(
        id = this.id,
        insuranceInfo = SimpleReceptionResponse.InsuranceInfo(
            insuranceNumber = this.insuranceInfo.insuranceNumber,
            coverageId = this.insuranceInfo.coverageId,
        ),
        patientInfo = SimpleReceptionResponse.PatientInfo(
            name = this.patientInfo.name.masked,
            age = this.patientInfo.age,
            sex = this.patientInfo.sex,
            primaryContact = SimpleReceptionResponse.Contact(
                phoneNumber = this.patientInfo.primaryContact.partialEncryptedPhoneNumber.toString(),
                relationshipWithPatient = this.patientInfo.primaryContact.relationshipWithPatient,
            ),
        ),
        accidentInfo = SimpleReceptionResponse.AccidentInfo(
            accidentNumber = this.accidentInfo.accidentNumber,
            hospitalRoomInfo = SimpleReceptionResponse.HospitalAndRoomInfo(
                state = this.accidentInfo.hospitalAndRoomInfo.state,
                city = this.accidentInfo.hospitalAndRoomInfo.city,
                hospitalAndRoom = this.accidentInfo.hospitalAndRoomInfo.hospitalAndRoom,
            ),
        ),
        caregivingManagerInfo = this.caregivingManagerInfo?.let {
            SimpleReceptionResponse.CaregivingManagerInfo(
                organizationType = it.organizationType,
                organizationId = it.organizationId,
                managingUserId = it.managingUserId,
            )
        },
        progressingStatus = this.progressingStatus,
        desiredCaregivingStartDate = this.desiredCaregivingStartDate,
        urgency = this.urgency,
        desiredCaregivingPeriod = this.desiredCaregivingPeriod,
        periodType = this.periodType,
        receivedDateTime = this.receivedDateTime.intoUtcOffsetDateTime()
    )

    @GetMapping("/{reception-id}")
    fun getReception(
        @PathVariable("reception-id") receptionId: String,
        @RequestParam("unmasked-property", defaultValue = "") unmaskedProperties: Set<UnmaskablePersonalProperty>,
        subject: Subject,
    ) = ResponseEntity.ok(
        receptionByIdQueryHandler.getReception(
            ReceptionByIdQuery(
                receptionId = receptionId,
                subject = subject,
            )
        ) {
            it.intoDetailResponse(unmaskedProperties, subject)
        }
    )

    private fun Reception.intoDetailResponse(
        unmaskedProperties: Set<UnmaskablePersonalProperty>,
        subject: Subject,
    ): DetailReceptionResponse {
        if (unmaskedProperties.contains(UnmaskablePersonalProperty.PATIENT_NAME)) {
            this.inDecryptionContext(decryptor, subject) { decryptPatientName() }
        }
        if (unmaskedProperties.contains(UnmaskablePersonalProperty.PATIENT_PRIMARY_PHONE_NUMBER)) {
            this.inDecryptionContext(decryptor, subject) { decryptPrimaryContact() }
        }
        if (unmaskedProperties.contains(UnmaskablePersonalProperty.PATIENT_SECONDARY_PHONE_NUMBER)) {
            this.inDecryptionContext(decryptor, subject) { decryptSecondaryContact() }
        }

        return DetailReceptionResponse(
            id = this.id,
            insuranceInfo = this.insuranceInfo.let { insuranceInfo ->
                DetailReceptionResponse.InsuranceInfo(
                    insuranceNumber = insuranceInfo.insuranceNumber,
                    subscriptionDate = insuranceInfo.subscriptionDate,
                    coverageId = insuranceInfo.coverageId,
                    caregivingLimitPeriod = insuranceInfo.caregivingLimitPeriod,
                )
            },
            patientInfo = this.patientInfo.let { patientInfo ->
                DetailReceptionResponse.PatientInfo(
                    name = patientInfo.name.toString(),
                    nickname = patientInfo.nickname,
                    age = patientInfo.age,
                    sex = patientInfo.sex,
                    weight = patientInfo.weight,
                    height = patientInfo.height,
                    primaryContact = patientInfo.primaryContact.intoResponseDataSet(),
                    secondaryContact = patientInfo.secondaryContact?.intoResponseDataSet(),
                )
            },
            accidentInfo = this.accidentInfo.let { accidentInfo ->
                DetailReceptionResponse.AccidentInfo(
                    accidentNumber = accidentInfo.accidentNumber,
                    accidentDateTime = accidentInfo.accidentDateTime.intoUtcOffsetDateTime(),
                    claimType = accidentInfo.claimType,
                    patientDescription = accidentInfo.patientDescription,
                    admissionDateTime = accidentInfo.admissionDateTime.intoUtcOffsetDateTime(),
                    hospitalRoomInfo = DetailReceptionResponse.HospitalRoomInfo(
                        state = accidentInfo.hospitalAndRoomInfo.state,
                        city = accidentInfo.hospitalAndRoomInfo.city,
                        hospitalAndRoom = accidentInfo.hospitalAndRoomInfo.hospitalAndRoom,
                    ),
                )
            },
            insuranceManagerInfo = this.insuranceManagerInfo.let { insuranceManagerInfo ->
                DetailReceptionResponse.InsuranceManagerInfo(
                    branchName = insuranceManagerInfo.branchName,
                    receptionistName = insuranceManagerInfo.receptionistName,
                    phoneNumber = insuranceManagerInfo.phoneNumber,
                )
            },
            caregivingManagerInfo = this.caregivingManagerInfo?.let { caregivingManagerInfo ->
                DetailReceptionResponse.CaregivingManagerInfo(
                    organizationType = caregivingManagerInfo.organizationType,
                    organizationId = caregivingManagerInfo.organizationId,
                    managingUserId = caregivingManagerInfo.managingUserId,
                )
            },
            registerManagerInfo = this.registerManagerInfo.let { registerManagerInfo ->
                DetailReceptionResponse.RegisterManagerInfo(
                    managingUserId = registerManagerInfo.managingUserId,
                )
            },
            desiredCaregivingStartDate = this.desiredCaregivingStartDate,
            urgency = this.urgency,
            desiredCaregivingPeriod = this.desiredCaregivingPeriod,
            additionalRequests = this.additionalRequests,
            expectedCaregivingStartDate = this.expectedCaregivingStartDate,
            expectedCaregivingLimitDate = this.expectedCaregivingLimitDate,
            progressingStatus = this.progressingStatus,
            periodType = this.periodType,
            receivedDateTime = this.receivedDateTime.intoUtcOffsetDateTime(),
            reasonForCancellation = this.reasonForCancellation,
            canceledDateTime = this.canceledDateTime?.intoUtcOffsetDateTime(),
            notifyCaregivingProgress = this.notifyCaregivingProgress,
        )
    }

    private fun EncryptedPatientInfo.EncryptedContact.intoResponseDataSet(): DetailReceptionResponse.Contact {
        return DetailReceptionResponse.Contact(
            phoneNumber = this.partialEncryptedPhoneNumber.toString(),
            relationshipWithPatient = this.relationshipWithPatient,
        )
    }

    @PutMapping("/{reception-id}")
    fun editReception(
        @PathVariable("reception-id") receptionId: String,
        @RequestBody payload: ReceptionEditingRequest,
        subject: Subject,
    ): ResponseEntity<Unit> {
        receptionEditingCommandHandler.editReception(
            ReceptionByIdQuery(
                receptionId = receptionId,
                subject = subject,
            ),
            payload.intoCommand(subject),
        )
        return ResponseEntity.noContent().build()
    }

    private fun ReceptionEditingRequest.intoCommand(subject: Subject) = ReceptionEditingCommand(
        insuranceInfo = this.insuranceInfo.let {
            InsuranceInfo(
                insuranceNumber = it.insuranceNumber,
                subscriptionDate = it.subscriptionDate,
                coverageId = it.coverageId,
                caregivingLimitPeriod = it.caregivingLimitPeriod,
            )
        },
        patientInfo = this.patientInfo.let {
            PatientInfo(
                name = it.name,
                nickname = it.nickname,
                age = it.age,
                sex = it.sex,
                height = it.height,
                weight = it.weight,
                primaryContact = it.primaryContact.intoCommandDataSet(),
                secondaryContact = it.secondaryContact?.intoCommandDataSet(),
            )
        },
        accidentInfo = this.accidentInfo.let {
            AccidentInfo(
                accidentNumber = it.accidentNumber,
                accidentDateTime = it.accidentDateTime.atLocalTimeZone(),
                claimType = it.claimType,
                patientDescription = it.patientDescription,
                admissionDateTime = it.admissionDateTime.atLocalTimeZone(),
                hospitalAndRoomInfo = it.hospitalRoomInfo.intoCommandDataSet(),
            )
        },
        caregivingManagerInfo = this.caregivingManagerInfo?.let {
            CaregivingManagerInfo(
                organizationType = it.organizationType,
                organizationId = it.organizationId,
                managingUserId = it.managingUserId,
            )
        },
        desiredCaregivingStartDate = this.desiredCaregivingStartDate,
        desiredCaregivingPeriod = this.desiredCaregivingPeriod,
        additionalRequests = this.additionalRequests,
        expectedCaregivingLimitDate = this.expectedCaregivingLimitDate,
        progressingStatus = this.progressingStatus,
        reasonForCancellation = this.reasonForCancellation,
        notifyCaregivingProgress = notifyCaregivingProgress,
        expectedCaregivingStartDate = this.expectedCaregivingStartDate,
        subject = subject,
    )

    private fun ReceptionEditingRequest.Contact.intoCommandDataSet() = PatientInfo.Contact(
        phoneNumber = this.phoneNumber,
        relationshipWithPatient = this.relationshipWithPatient,
    )

    private fun ReceptionEditingRequest.HospitalAndRoomInfo.intoCommandDataSet() = AccidentInfo.HospitalAndRoomInfo(
        state = this.state,
        city = this.city,
        hospitalAndRoom = this.hospitalAndRoom,
    )

    @ExceptionHandler(ReferenceCoverageNotExistsException::class)
    fun handleReferenceCoverageNotExistsException(e: ReferenceCoverageNotExistsException) = ResponseEntity
        .status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(
            GeneralErrorResponse(
                message = "요청에 포함된 가입 담보가 존재하지 않습니다.",
                errorType = "REFERENCE_COVERAGE_NOT_EXISTS",
                data = NotExistingReferenceCoverageData(e.referenceCoverageId),
            )
        )

    @ExceptionHandler(InvalidReceptionProgressingStatusTransitionException::class)
    fun handleInvalidReceptionProgressingStatusTransitionException(e: InvalidReceptionProgressingStatusTransitionException) =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(
                GeneralErrorResponse(
                    message = "지정한 상태로 접수를 변경할 수 없습니다.",
                    errorType = "INVALID_RECEPTION_STATE_TRANSITION",
                    data = InvalidReceptionProgressingStatusEnteredData(
                        currentReceptionProgressingStatus = e.currentProgressingStatus,
                        enteredReceptionProgressingStatus = e.enteredProgressingStatus,
                    ),
                )
            )
}
