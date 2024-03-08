package kr.caredoc.careinsurance

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import java.time.LocalDateTime

fun <T> withFixedClock(
    fixedDateTime: LocalDateTime,
    block: () -> T
): T {
    mockkObject(Clock)
    every { Clock.now() } returns fixedDateTime
    every { Clock.today() } returns fixedDateTime.toLocalDate()

    val result = block()

    unmockkObject(Clock)

    return result
}
