package kr.caredoc.careinsurance.caregiving.exception

class ReferenceCaregivingRoundNotExistException(
    val referenceCaregivingRoundId: String,
    override val cause: Throwable? = null,
) : RuntimeException("참조하고자 하는 CaregivingRound($referenceCaregivingRoundId)가 존재하지 않습니다.")
