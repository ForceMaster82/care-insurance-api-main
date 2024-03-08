package kr.caredoc.careinsurance.web.application.sentry

import io.sentry.SamplingContext

interface SamplingFilter {
    /**
     * Filter context.
     *
     * @param context context to filter
     * @return filtering result. if true, the filter has determined that this context could be sampled.
     */
    fun filter(context: SamplingContext): Boolean
}
