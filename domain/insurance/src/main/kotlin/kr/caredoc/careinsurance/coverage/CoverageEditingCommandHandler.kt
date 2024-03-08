package kr.caredoc.careinsurance.coverage

interface CoverageEditingCommandHandler {
    fun editCoverage(command: CoverageEditingCommand)
}
