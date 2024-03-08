package kr.caredoc.careinsurance.coverage

class CoverageNameDuplicatedException(duplicatedCoverageName: String) :
    RuntimeException("Coverage(name: $duplicatedCoverageName)는 이미 등록되어있습니다.")
