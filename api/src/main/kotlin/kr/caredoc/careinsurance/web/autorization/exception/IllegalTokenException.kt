package kr.caredoc.careinsurance.web.autorization.exception

class IllegalTokenException(message: String, cause: Throwable?) :
    RuntimeException(message, cause) {
    constructor(message: String) : this(message, null)
}
