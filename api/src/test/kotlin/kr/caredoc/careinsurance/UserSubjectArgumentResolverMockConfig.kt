package kr.caredoc.careinsurance

import kr.caredoc.careinsurance.web.autorization.UserSubjectArgumentResolver
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class UserSubjectArgumentResolverMockConfig {
    @Bean
    fun userSubjectArgumentResolver() = relaxedMock<UserSubjectArgumentResolver>()
}
