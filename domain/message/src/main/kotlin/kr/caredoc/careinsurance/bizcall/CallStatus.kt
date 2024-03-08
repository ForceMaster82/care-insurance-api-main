package kr.caredoc.careinsurance.bizcall

enum class CallStatus {
    READY, // 발신전
    FETCHING, // 발신 준비
    PROGRESS, // 발신 진행중
    RETRYING, // 재발신 진행중
    DONE, // 발신 완료
    FAIL, // 발신 실패
    STOP, // 발신 정지
    NOT_SENT // 미발신
}
