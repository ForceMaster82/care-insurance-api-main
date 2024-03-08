package kr.caredoc.careinsurance.caregiving

import java.time.LocalDateTime

class IllegalCaregivingPeriodEnteredException(
    val targetCaregivingRoundId: String,
    val enteredStartDateTime: LocalDateTime,
) : RuntimeException("CaregivingRound($targetCaregivingRoundId)에 잘못된 간병 시작 일시($enteredStartDateTime)를 입력하려 시도했습니다.")
