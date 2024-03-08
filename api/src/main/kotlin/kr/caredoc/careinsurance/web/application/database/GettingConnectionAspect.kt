package kr.caredoc.careinsurance.web.application.database

import io.sentry.Sentry
import kr.caredoc.careinsurance.web.application.sentry.SentrySpannedConnection
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.springframework.stereotype.Component
import java.sql.Connection

@Component
@Aspect
class GettingConnectionAspect {
    @Pointcut("execution(java.sql.Connection javax.sql.DataSource.getConnection(..))")
    fun getConnection() {
        // pointcut declaration
    }

    @Around("getConnection()")
    fun recordSentrySpanInGetConnection(joinPoint: ProceedingJoinPoint): Any? {
        val childSpan = Sentry.getSpan()?.startChild(joinPoint.signature.name, joinPoint.signature.toLongString())

        return try {
            val returnedValue = joinPoint.proceed()

            (returnedValue as? Connection)?.let {
                SentrySpannedConnection(it)
            } ?: returnedValue
        } finally {
            childSpan?.finish()
        }
    }
}
