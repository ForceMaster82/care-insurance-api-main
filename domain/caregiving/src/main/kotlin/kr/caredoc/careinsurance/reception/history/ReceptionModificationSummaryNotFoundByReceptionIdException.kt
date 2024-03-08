package kr.caredoc.careinsurance.reception.history

class ReceptionModificationSummaryNotFoundByReceptionIdException(enteredReceptionId: String) :
    RuntimeException("ReceptionModificationSummary(receptionId: $enteredReceptionId)는 존재하지 않습니다.")
