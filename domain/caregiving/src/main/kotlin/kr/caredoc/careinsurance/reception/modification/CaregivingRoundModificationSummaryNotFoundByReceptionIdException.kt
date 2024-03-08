package kr.caredoc.careinsurance.reception.modification

class CaregivingRoundModificationSummaryNotFoundByReceptionIdException(enteredReceptionId: String) :
    RuntimeException("CaregivingRoundModificationSummary(receptionId: $enteredReceptionId)는 존재하지 않습니다.")
