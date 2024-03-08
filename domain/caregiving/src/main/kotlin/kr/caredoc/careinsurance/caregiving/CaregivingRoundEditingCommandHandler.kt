package kr.caredoc.careinsurance.caregiving

interface CaregivingRoundEditingCommandHandler {
    fun editCaregivingRound(query: CaregivingRoundByIdQuery, command: CaregivingRoundEditingCommand)
}
