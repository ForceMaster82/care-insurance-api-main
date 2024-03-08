package kr.caredoc.careinsurance.billing

class BillingNotFoundByCaregivingRoundIdException(val caregivingRoundId: String) :
    RuntimeException("Billing(caregivingRoundId: $caregivingRoundId)가 존재하지 않습니다.")
