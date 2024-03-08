package kr.caredoc.careinsurance.reception

class ReferenceCoverageNotExistsException(val referenceCoverageId: String, override val cause: Throwable? = null) :
    RuntimeException("보장받고자 하는 Coverage($referenceCoverageId)가 존재하지 않습니다.", cause)
