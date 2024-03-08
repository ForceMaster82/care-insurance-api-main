package kr.caredoc.careinsurance.web.application

import kr.caredoc.careinsurance.web.autorization.UserSubjectArgumentResolver
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfiguration(
    private val userSubjectArgumentResolver: UserSubjectArgumentResolver,
    @Value("\${security.allowed-origins:}")
    private val allowedOrigins: List<String>,
) : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        super.addCorsMappings(registry)

        if (allowedOrigins.isEmpty()) {
            registry.addMapping("/**")
                .allowedMethods("*")
                .allowedHeaders("*")
                .exposedHeaders("*")
        } else {
            registry.addMapping("/**")
                .allowedOrigins(*allowedOrigins.toTypedArray())
                .allowedMethods("*")
                .allowedHeaders("*")
                .exposedHeaders("*")
        }
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(userSubjectArgumentResolver)
        super.addArgumentResolvers(resolvers)
    }
}
