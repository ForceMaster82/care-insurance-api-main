package kr.caredoc.careinsurance.bizcall

data class BizcallBadRequestException(val bizcallErrorResult: BizcallErrorResult?) :
    RuntimeException("${bizcallErrorResult?.errorCode}: ${bizcallErrorResult?.errorMessage}")
