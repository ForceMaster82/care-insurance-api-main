package kr.caredoc.careinsurance.security.personaldata

import jakarta.persistence.EntityManagerFactory
import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.reception.Reception
import kr.caredoc.careinsurance.reception.ReceptionModified
import kr.caredoc.careinsurance.security.accesscontrol.SystemUser
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.database.JpaCursorItemReader
import org.springframework.batch.item.database.JpaItemWriter
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Configuration
class PersonalDataExpirationJobConfiguration {
    @Bean
    fun personalDataExpirationJob(
        jobRepository: JobRepository,
        personalDataExpirationStep: Step,
        @Value("\${spring.batch.job.force-run}")
        forceRun: Boolean,
    ): Job {
        val jobBuilder = JobBuilder("personalDataExpirationJob", jobRepository)
            .start(personalDataExpirationStep)

        if (forceRun) {
            jobBuilder.incrementer(RunIdIncrementer())
        }

        return jobBuilder.build()
    }

    @Bean
    @JobScope
    fun personalDataExpirationStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        reader: JpaCursorItemReader<Reception>,
        writer: JpaItemWriter<Reception>,
        applicationEventPublisher: ApplicationEventPublisher,
    ) = StepBuilder("personalDataExpirationStep", jobRepository)
        .chunk<Reception, Reception>(100, transactionManager)
        .reader(reader)
        .processor {
            it.apply {
                deletePatientPersonalData(SystemUser, ReceptionModified.Cause.SYSTEM)
                it.domainEvents.forEach { event ->
                    applicationEventPublisher.publishEvent(event)
                }
            }
        }
        .writer(writer)
        .build()

    @Bean
    @JobScope
    fun expiredPersonalDataReceptionReader(
        entityManagerFactory: EntityManagerFactory,
        @Value("#{jobParameters[processingDate]}")
        processingDateAsString: String,
    ): JpaCursorItemReader<Reception> {
        val processingDate = processingDateAsString.let {
            LocalDate.parse(it)
        } ?: Clock.today()
        val sixMonthBefore = processingDate.minusMonths(6).plusDays(1).atStartOfDay()
        return JpaCursorItemReaderBuilder<Reception>()
            .name("Reception")
            .entityManagerFactory(entityManagerFactory)
            .queryString(
                """
                    SELECT r
                    FROM Reception r
                    WHERE r.progressingStatus IN (CANCELED, CANCELED_BY_PERSONAL_CAREGIVER, CANCELED_BY_MEDICAL_REQUEST, COMPLETED)
                        AND r.receivedDateTime < '${sixMonthBefore.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}'
                        AND (
                            r.patientInfo.name.hashed != ''
                            OR r.patientInfo.primaryContact.maskedPhoneNumber != ''
                            OR (
                                r.patientInfo.secondaryContact.maskedPhoneNumber != ''
                                AND r.patientInfo.secondaryContact.maskedPhoneNumber IS NOT NULL
                            )
                        )
                """.trimIndent()
            )
            .build()
    }

    @Bean
    @JobScope
    fun receptionWriter(entityManagerFactory: EntityManagerFactory): JpaItemWriter<Reception> {
        JobParametersBuilder()
            .toJobParameters()
        return JpaItemWriterBuilder<Reception>()
            .entityManagerFactory(entityManagerFactory)
            .build()
    }
}
