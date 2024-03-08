package kr.caredoc.careinsurance.file

interface GeneratingOpenedFileUrlCommandHandler {
    fun generateOpenedUrl(query: FileByUrlQuery, command: GeneratingOpenedFileUrlCommand): OpenedUrl?
}
