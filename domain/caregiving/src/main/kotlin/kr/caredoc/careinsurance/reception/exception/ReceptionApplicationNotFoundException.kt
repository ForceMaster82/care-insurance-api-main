package kr.caredoc.careinsurance.reception.exception

class ReceptionApplicationNotFoundException(val receptionId: String) : RuntimeException("Reception 간병인 신청서를 찾을 수 없습니다.")
