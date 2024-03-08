package kr.caredoc.careinsurance.coverage

interface CoverageCreationCommandHandler {
    fun createCoverage(command: CoverageCreationCommand): CoverageCreationResult
}
