package kr.caredoc.careinsurance.web.application.sentry

import io.sentry.SamplingContext
import io.sentry.SentryOptions.TracesSamplerCallback

class FilteredSamplerCallback(
    private val samplingRate: Double,
    private val filters: Collection<SamplingFilter>,
) : TracesSamplerCallback {
    override fun sample(samplingContext: SamplingContext): Double? {
        if (filters.all { it.filter(samplingContext) }) {
            return samplingRate
        }

        return 0.0
    }
}
