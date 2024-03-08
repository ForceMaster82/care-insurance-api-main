package kr.caredoc.careinsurance.phonenumber

class InvalidPhoneNumberException(val enteredPhoneNumber: String) :
    Throwable("잘못된 핸드폰 번호($enteredPhoneNumber)가 입력되었습니다.")
