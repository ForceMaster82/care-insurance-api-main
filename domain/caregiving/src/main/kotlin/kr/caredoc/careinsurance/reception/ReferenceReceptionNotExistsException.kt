package kr.caredoc.careinsurance.reception

class ReferenceReceptionNotExistsException(
    val referenceReceptionId: String,
    override val cause: Throwable? = null,
) : RuntimeException("참조하고자 하는 Reception($referenceReceptionId)이 존재하지 않습니다.")
