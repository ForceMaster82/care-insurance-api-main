package kr.caredoc.careinsurance.reception

interface ReceptionEditingCommandHandler {
    fun editReception(query: ReceptionByIdQuery, command: ReceptionEditingCommand)
}
