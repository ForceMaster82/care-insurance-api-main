package kr.caredoc.careinsurance.web.application.log

import io.sentry.Sentry
import kr.caredoc.careinsurance.Clock
import kr.caredoc.careinsurance.security.IncludingSecuredData
import kr.caredoc.careinsurance.security.personaldata.IncludingPersonalData
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import java.time.Duration

@Component
@Aspect
class ServiceMethodAspect {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Pointcut("@within(org.springframework.stereotype.Service)")
    fun serviceMethod() {
        // pointcut declaration
    }

    @Around("serviceMethod()")
    fun recordSentrySpan(joinPoint: ProceedingJoinPoint): Any? {
        val childSpan = Sentry.getSpan()?.startChild(joinPoint.signature.name, joinPoint.signature.toLongString())

        return try {
            joinPoint.proceed()
        } finally {
            childSpan?.finish()
        }
    }

    @Around("serviceMethod()")
    fun logServiceMethod(joinPoint: ProceedingJoinPoint): Any? {
        val argumentsByKey = extractArguments(joinPoint).mapIndexed { i, argument ->
            "argument$i(${argument.parameterName})" to argument
        }.toMap()
        val calledAt = Clock.now()

        try {
            val result = joinPoint.proceed()
            val elapsedTime = Duration.between(calledAt, Clock.now()).toMillis()

            withMdc(elapsedTime, argumentsByKey) {
                logger.info("service method {} successfully proceed.", joinPoint.signature)
            }

            return result
        } catch (e: Exception) {
            val elapsedTime = Duration.between(calledAt, Clock.now()).toMillis()

            withMdc(elapsedTime, argumentsByKey) {
                logger.warn("service method {} throws exception.", joinPoint.signature)
            }

            throw e
        }
    }

    private fun extractArguments(joinPoint: ProceedingJoinPoint): List<Argument> {
        val method = (joinPoint.signature as? MethodSignature)
            ?.method
        val parameterNames = method?.parameters?.map { it.name }
        val parameterAnnotations = method?.parameterAnnotations

        if (parameterAnnotations == null) {
            logger.warn(
                "couldn't recognize parameter annotations from service method {}. skip argument logging",
                joinPoint.signature,
            )
            return listOf()
        }

        if (parameterNames == null) {
            logger.warn(
                "couldn't recognize parameter names from service method {}. skip argument logging",
                joinPoint.signature,
            )
            return listOf()
        }

        return joinPoint.args.mapIndexed { i, arg ->
            Argument(
                parameterName = parameterNames.getOrNull(i) ?: "",
                parameterAnnotations = parameterAnnotations.getOrNull(i)?.toSet() ?: setOf(),
                value = arg,
            )
        }
    }

    private fun fillMdc(elapsedTime: Long, arguments: Map<String, Argument>) {
        MDC.put("elapsedTime", elapsedTime.toString() + "ms")
        arguments
            .forEach {
                val argumentValue = if (it.value.hasPersonalData) {
                    "PERSONAL"
                } else if (it.value.isSecured) {
                    "SECURED"
                } else {
                    it.value.value.toString()
                }
                MDC.put(it.key, argumentValue)
            }
    }

    private fun clearMdc(filledArguments: Map<String, Argument>) {
        MDC.remove("elapsedTime")
        filledArguments.keys.forEach { MDC.remove(it) }
    }

    private fun withMdc(elapsedTime: Long, arguments: Map<String, Argument>, block: () -> Unit) {
        fillMdc(elapsedTime, arguments)
        block()
        clearMdc(arguments)
    }

    private data class Argument(
        val parameterName: String,
        val parameterAnnotations: Set<Annotation>,
        val value: Any?,
    ) {
        val hasPersonalData = parameterAnnotations.any { it.annotationClass == IncludingPersonalData::class }
        val isSecured = parameterAnnotations.any { it.annotationClass == IncludingSecuredData::class }
    }
}
