package kr.caredoc.careinsurance

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.core.annotation.AliasFor
import kotlin.reflect.KClass

@WebMvcTest
@Target(AnnotationTarget.CLASS)
@Import(UserSubjectArgumentResolverMockConfig::class)
annotation class CareInsuranceWebMvcTest(
    @get:AliasFor(annotation = WebMvcTest::class)
    vararg val controllers: KClass<*>,
)
