package kr.caredoc.careinsurance.aws.s3

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner

@Configuration
class S3Config {
    val region: Region = Region.AP_NORTHEAST_2

    @Bean
    fun s3Client(): S3Client = S3Client.builder()
        .credentialsProvider(DefaultCredentialsProvider.create())
        .region(region)
        .build()

    @Bean
    fun s3PreSigner(): S3Presigner = S3Presigner.builder()
        .credentialsProvider(DefaultCredentialsProvider.create())
        .region(region)
        .build()
}
