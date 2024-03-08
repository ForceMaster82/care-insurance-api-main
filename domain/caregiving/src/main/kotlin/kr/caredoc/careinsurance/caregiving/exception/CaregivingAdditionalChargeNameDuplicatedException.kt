package kr.caredoc.careinsurance.caregiving.exception

class CaregivingAdditionalChargeNameDuplicatedException(val duplicatedAdditionalChargeNames: Set<String>) :
    RuntimeException("중복된 간병비 기타 비용 계정과목이 존재합니다.")
