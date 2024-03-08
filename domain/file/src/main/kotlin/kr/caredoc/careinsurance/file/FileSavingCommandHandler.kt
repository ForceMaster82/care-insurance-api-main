package kr.caredoc.careinsurance.file

interface FileSavingCommandHandler {
    fun saveFile(command: FileSavingCommand): FileSavingResult
}
