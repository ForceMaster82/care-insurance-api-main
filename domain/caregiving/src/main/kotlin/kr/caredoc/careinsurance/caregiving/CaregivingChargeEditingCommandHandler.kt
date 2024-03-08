package kr.caredoc.careinsurance.caregiving

interface CaregivingChargeEditingCommandHandler {
    fun createOrEditCaregivingCharge(query: CaregivingRoundByIdQuery, command: CaregivingChargeEditingCommand)
}
