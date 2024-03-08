package kr.caredoc.careinsurance.reconciliation

interface ReconciliationEditingCommandHandler {
    fun editReconciliations(commands: Collection<Pair<ReconciliationByIdQuery, ReconciliationEditingCommand>>)
}
