package kr.caredoc.careinsurance.web.modification

import kr.caredoc.careinsurance.intoUtcOffsetDateTime
import kr.caredoc.careinsurance.modification.DynamicType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DynamicTypedValueFormatter {
    fun format(value: String, type: DynamicType): Any {
        return when (type) {
            DynamicType.NUMBER -> value.toInt()
            DynamicType.STRING -> value
            DynamicType.BOOLEAN -> value.toBoolean()
            DynamicType.DATETIME -> LocalDateTime.parse(value).intoUtcOffsetDateTime()
            DynamicType.DATE -> value.format(DateTimeFormatter.ISO_LOCAL_DATE)
        }
    }
}
