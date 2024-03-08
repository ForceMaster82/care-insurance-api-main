package kr.caredoc.careinsurance.settlement

class ReferenceSettlementNotExistsException(
    val referenceSettlementId: String,
    override val cause: Throwable? = null,
) : RuntimeException("참조하고자 하는 Settlement($referenceSettlementId)가 존재하지 않습니다.")
