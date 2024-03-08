package kr.caredoc.careinsurance.reception.modification

class CaregivingChargeModificationSummaryNotFoundByReceptionIdException(enteredReceptionId: String) :
    RuntimeException("CaregivingChargeModificationSummary(receptionId: $enteredReceptionId)는 존재하지 않습니다.")
