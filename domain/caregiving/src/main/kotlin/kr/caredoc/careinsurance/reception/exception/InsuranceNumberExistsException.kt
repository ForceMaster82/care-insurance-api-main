package kr.caredoc.careinsurance.reception.exception

class InsuranceNumberExistsException (val insuranceNumber: String) :
    RuntimeException("증권번호($insuranceNumber)는 이미 등록되어 있습니다.")
