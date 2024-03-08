package kr.caredoc.careinsurance.reception.exception

class ReceptionNotFoundByIdException(val receptionId: String) :
    RuntimeException("Reception($receptionId)이 존재하지 않습니다.")
