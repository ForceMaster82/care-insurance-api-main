package kr.caredoc.careinsurance.billing

interface DownloadCertificateCommandHandler {
    fun downloadCertification(command: DownloadCertificateCommand): ByteArray
}
