package kr.caredoc.careinsurance.aws.ses

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

//@Configuration
class SesConfig {
//    @Bean
    fun sesEmailClient(): AmazonSimpleEmailService = AmazonSimpleEmailServiceClientBuilder.defaultClient()
}
