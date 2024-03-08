package kr.caredoc.careinsurance.file

import jakarta.persistence.EntityManagerFactory
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
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

@Configuration
class S3BatchDeleteJobConfig {
    @Bean
    fun s3BatchDeleteJob(
        jobRepository: JobRepository,
        s3BatchDeleteStep: Step,
        @Value("\${spring.batch.job.force-run}")
        forceRun: Boolean,
    ): Job {
        val jobBuilder = JobBuilder("s3BatchDeleteJob", jobRepository)
            .start(s3BatchDeleteStep)

        if (forceRun) {
            jobBuilder.incrementer(RunIdIncrementer())
        }

        return jobBuilder.build()
    }

    @Bean
    @JobScope
    fun s3BatchDeleteStep(
        amazonS3: S3Client,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        reader: JpaCursorItemReader<FileMeta>,
        writer: JpaItemWriter<FileMeta>,
        applicationEventPublisher: ApplicationEventPublisher,
    ) = StepBuilder("s3BatchDeleteStep", jobRepository)
        .chunk<FileMeta, FileMeta>(100, transactionManager)
        .reader(reader)
        .processor {
            it.markAsDeleted()
            try {
                amazonS3.deleteObject(
                    DeleteObjectRequest.builder()
                        .bucket(it.bucket)
                        .key(it.path)
                        .build()
                )
            } catch (e: NoSuchKeyException) {
                // do nothing, because file already deleted
            }

            it
        }
        .writer(writer)
        .build()

    @Bean
    @JobScope
    fun softDeletedFileMetaReader(
        entityManagerFactory: EntityManagerFactory,
    ): JpaCursorItemReader<FileMeta> {
        return JpaCursorItemReaderBuilder<FileMeta>()
            .name("FileMeta")
            .entityManagerFactory(entityManagerFactory)
            .queryString(
                """
                    SELECT fm
                    FROM FileMeta fm
                    WHERE fm.status = "TO_BE_DELETED"
                """.trimIndent()
            )
            .build()
    }

    @Bean
    @JobScope
    fun fileMetaWriter(entityManagerFactory: EntityManagerFactory): JpaItemWriter<FileMeta> {
        JobParametersBuilder()
            .toJobParameters()
        return JpaItemWriterBuilder<FileMeta>()
            .entityManagerFactory(entityManagerFactory)
            .build()
    }
}
