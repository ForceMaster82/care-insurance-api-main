package kr.caredoc.careinsurance

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.extensions.spring.SpringExtension
import java.util.TimeZone

class ProjectConfig : AbstractProjectConfig() {
    override fun extensions() = listOf(SpringExtension)
    override suspend fun beforeProject() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
    }
}
