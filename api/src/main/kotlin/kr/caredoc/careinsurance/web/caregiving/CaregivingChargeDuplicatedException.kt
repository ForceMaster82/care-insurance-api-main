package kr.caredoc.careinsurance.web.caregiving

class CaregivingChargeDuplicatedException(val caregivingRoundId: String) :
    RuntimeException("간병비 산정을 중복 입력할 수 없습니다.(caregivingRoundId:$caregivingRoundId)")
