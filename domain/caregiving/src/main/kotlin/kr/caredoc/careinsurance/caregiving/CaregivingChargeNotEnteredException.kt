package kr.caredoc.careinsurance.caregiving

class CaregivingChargeNotEnteredException(val enteredCaregivingRoundId: String) :
    RuntimeException("CaregivingCharge(caregivingRoundId: $enteredCaregivingRoundId)는 입력된적이 없습니다.")
