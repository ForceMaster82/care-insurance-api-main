package kr.caredoc.careinsurance.caregiving.exception

class CaregivingChargeEditingDeniedException(val caregivingChargeId: String) :
    RuntimeException("CaregivingCharge(id:$caregivingChargeId)는 간병비 산정 확정된 상태이므로 수정 처리를 할 수 없습니다.")
