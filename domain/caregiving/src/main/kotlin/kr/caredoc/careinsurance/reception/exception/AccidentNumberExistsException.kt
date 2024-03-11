package kr.caredoc.careinsurance.reception.exception

class AccidentNumberExistsException(val accidentNumber: String) :
    RuntimeException("사고번호($accidentNumber)는 이미 등록되어 있습니다.")
