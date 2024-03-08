package kr.caredoc.careinsurance

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.time.format.DateTimeFormatter

@SpringBootApplication
class CareInsuranceBatchApplication

fun main(args: Array<String>) {
    runApplication<CareInsuranceBatchApplication>(*interceptArgs(args))
}

class JobNotExistsException(jobName: String) : RuntimeException("Job(jobName: $jobName) is not configured.")

enum class Job(
    val jobName: String,
    val requiredArgs: Set<BatchParameter>
) {
    PERSONAL_DATA_EXPIRATION(
        jobName = "personalDataExpirationJob",
        requiredArgs = setOf(BatchParameter.PROCESSING_DATE),
    ),
    S3_BATCH_DELETE_JOB(
        jobName = "s3BatchDeleteJob",
        requiredArgs = setOf(BatchParameter.PROCESSING_DATE),
    );

    companion object {
        fun parse(jobName: String): Job {
            return Job.values().find { it.jobName == jobName } ?: throw JobNotExistsException(jobName)
        }
    }
}

enum class BatchParameter {
    PROCESSING_DATE,
}

private fun interceptArgs(args: Array<String>): Array<String> {
    val runningJobs = parseRunningJobs(args)
    val requiredArgs = runningJobs.asSequence().flatMap { it.requiredArgs }.toSet()

    if (requiredArgs.contains(BatchParameter.PROCESSING_DATE)) {
        return addProcessingDateAsToday(args)
    }

    return args
}

private fun parseRunningJobs(args: Array<String>): Set<Job> {
    val jobNameArguments = args.filter {
        it.startsWith("--job.name=")
    }

    if (jobNameArguments.isEmpty()) {
        return Job.values().toSet()
    }

    return jobNameArguments.map {
        Job.parse(it.split("=")[1])
    }.toSet()
}

private fun addProcessingDateAsToday(args: Array<String>): Array<String> {
    val processingDateArg = "processingDate=${Clock.today().format(DateTimeFormatter.ISO_LOCAL_DATE)}"
    return args + arrayOf(processingDateArg)
}
