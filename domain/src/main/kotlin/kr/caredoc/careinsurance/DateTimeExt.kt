package kr.caredoc.careinsurance

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

fun OffsetDateTime.atLocalTimeZone(): LocalDateTime = this
    .atZoneSameInstant(ZoneOffset.systemDefault())
    .toLocalDateTime()

fun LocalDateTime.intoUtcOffsetDateTime(): OffsetDateTime =
    ZonedDateTime.of(this, ZoneOffset.systemDefault())
        .withZoneSameInstant(ZoneOffset.UTC)
        .toOffsetDateTime()
