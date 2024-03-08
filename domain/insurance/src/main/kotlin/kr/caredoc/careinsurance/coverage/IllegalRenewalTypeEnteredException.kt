package kr.caredoc.careinsurance.coverage

class IllegalRenewalTypeEnteredException(val enteredRenewalType: RenewalType) :
    RuntimeException("잘못된 갱신 구분($enteredRenewalType)이 입력됐습니다.")
