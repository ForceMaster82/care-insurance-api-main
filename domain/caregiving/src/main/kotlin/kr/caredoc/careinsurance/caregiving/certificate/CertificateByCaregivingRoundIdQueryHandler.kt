package kr.caredoc.careinsurance.caregiving.certificate

interface CertificateByCaregivingRoundIdQueryHandler {
    fun getCertificate(query: CertificateByCaregivingRoundIdQuery): ByteArray
}
