package kr.caredoc.careinsurance

import java.time.LocalDate
import java.time.LocalDateTime

object Clock {
    fun now(): LocalDateTime = LocalDateTime.now()

    fun today(): LocalDate = LocalDate.now()
}
