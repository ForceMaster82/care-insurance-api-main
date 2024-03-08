package kr.caredoc.careinsurance.coverage

class SubscriptionYearDuplicatedException(
    duplicatedRenewalType: RenewalType,
    duplicatedSubscriptionYear: Int,
) : RuntimeException("Coverage(renewalType: $duplicatedRenewalType, subscriptionYear: $duplicatedSubscriptionYear)는 이미 등록되어있습니다.")
