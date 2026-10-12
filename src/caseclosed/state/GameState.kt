package caseclosed.state

import caseclosed.data.CaseCatalog
import caseclosed.model.CaseData
import caseclosed.model.CaseInvestigationState
import caseclosed.model.CasePack
import caseclosed.model.GlobalProgression

/**
 * Everything the game knows right now: which case is open, which files are
 * unlocked, and the investigation saved for each case.
 * Screens read this. Only [caseclosed.engine.GameEngine] replaces it.
 */
data class GameState(
    val progression: GlobalProgression = GlobalProgression(),
    val investigations: Map<String, CaseInvestigationState> = emptyMap(),
) {
    fun investigation(caseId: String = progression.activeCaseId): CaseInvestigationState =
        investigations.getValue(caseId)

    fun activePack(): CasePack = CaseCatalog.require(progression.activeCaseId)

    fun activeCase(): CaseData = activePack().data

    fun isCompleted(caseId: String): Boolean = caseId in progression.completedCaseIds

    fun unreadCount(): Int {
        val read = investigation().readMessageIds.toSet()
        return activeCase().phoneMessages.count { it.unread && it.id !in read }
    }

    fun isPinned(type: String, entityId: String): Boolean =
        investigation().boardNodes.any { it.type == type && it.entityId == entityId }

    fun withInvestigation(
        caseId: String = progression.activeCaseId,
        block: (CaseInvestigationState) -> CaseInvestigationState,
    ): GameState = copy(investigations = investigations + (caseId to block(investigation(caseId))))
}
