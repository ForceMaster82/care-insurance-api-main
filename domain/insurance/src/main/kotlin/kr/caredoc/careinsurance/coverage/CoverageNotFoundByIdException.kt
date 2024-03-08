package kr.caredoc.careinsurance.coverage

class CoverageNotFoundByIdException(val coverageId: String) : RuntimeException("Coverage($coverageId)가 존재하지 않습니다.")
