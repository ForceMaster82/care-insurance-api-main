package kr.caredoc.careinsurance.caregiving.exception

class UnknownCaregivingRoundInfoException(val caregivingRoundId: String) :
    RuntimeException("Caregiving Round(id:$caregivingRoundId)정보의 시작일자, 종료일자, 간병인 정보가 존재하지 않아 간병비 산정을 진행할 수 없습니다.")
