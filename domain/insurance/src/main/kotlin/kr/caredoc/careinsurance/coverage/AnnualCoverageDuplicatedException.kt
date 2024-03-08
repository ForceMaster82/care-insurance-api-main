package kr.caredoc.careinsurance.coverage

class AnnualCoverageDuplicatedException(val duplicatedYears: Set<Int>) : RuntimeException("중복된 간병비 보장 연도가 존재합니다.")
