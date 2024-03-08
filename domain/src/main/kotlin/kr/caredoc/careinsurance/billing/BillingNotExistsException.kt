package kr.caredoc.careinsurance.billing

class BillingNotExistsException(val billingId: String) : RuntimeException("[billing] 조회하고자 하는 청구 $billingId 가 없습니다.")
