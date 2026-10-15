package caseclosed.engine

import caseclosed.data.CaseCatalog
import caseclosed.model.CaseData
import caseclosed.model.CaseInvestigationState
import caseclosed.model.GlobalProgression
import caseclosed.model.Question
import caseclosed.model.Verdict
import caseclosed.state.GameState

/**
 * Changes [GameState] and writes it through [GameSaves].
 * Screens call these commands. They do not edit the investigation themselves.
 */
class GameEngine(
    private val saves: GameSaves,
    private val now: () -> Long,
) {
    private val ids = IdFactory(now)

    var state: GameState = GameState()
        private set

    val progression: GlobalProgression get() = state.progression

    fun investigation(): CaseInvestigationState = state.investigation()

    fun activeCase(): CaseData = state.activeCase()

    fun unreadCount(): Int = state.unreadCount()

    fun isPinned(type: String, entityId: String): Boolean = state.isPinned(type, entityId)

    fun isCompleted(caseId: String): Boolean = state.isCompleted(caseId)

    fun isStarted(caseId: String = progression.activeCaseId): Boolean = state.isStarted(caseId)

    fun load() {
        var progression = saves.loadProgress() ?: GlobalProgression()
        val known = CaseCatalog.packs.map { it.data.id }
        if (progression.activeCaseId !in known) progression = GlobalProgression()
        if ("case_001" !in progression.unlockedCaseIds) {
            progression = progression.copy(unlockedCaseIds = listOf("case_001") + progression.unlockedCaseIds)
        }
        val investigations = CaseCatalog.packs.associate { pack ->
            pack.data.id to (saves.loadCase(pack.data.id) ?: CaseCatalog.freshState(pack))
        }
        state = GameState(progression, investigations)
        saves.saveProgress(state.progression)
    }

    fun setActiveCase(caseId: String) {
        if (caseId !in state.progression.unlockedCaseIds) return
        if (CaseCatalog.packs.none { it.data.id == caseId }) return
        var investigations = state.investigations
        if (caseId !in investigations) {
            investigations = investigations + (caseId to CaseCatalog.freshState(CaseCatalog.require(caseId)))
        }
        state = state.copy(
            progression = state.progression.copy(activeCaseId = caseId),
            investigations = investigations,
        )
        saves.saveProgress(state.progression)
    }

    /**
     * Opens the active case from the title screen.
     * The first time it lays out the authored starting state; after that it keeps the saved investigation.
     */
    fun startCase() {
        val caseId = state.progression.activeCaseId
        if (state.isStarted(caseId)) return
        val fresh = CaseCatalog.freshState(CaseCatalog.require(caseId))
        val progression = state.progression.copy(
            startedCaseIds = state.progression.startedCaseIds + caseId,
        )
        state = state.copy(
            progression = progression,
            investigations = state.investigations + (caseId to fresh),
        )
        saves.saveCase(caseId, fresh)
        saves.saveProgress(progression)
    }

    fun markMessagesRead() {
        val ids = activeCase().phoneMessages.map { it.id }
        edit { InvestigationRules.markMessagesRead(it, ids) }
    }

    fun saveHighlight(
        id: String?,
        sourceType: String,
        sourceId: String,
        sectionId: String?,
        questionId: String?,
        text: String,
        note: String,
    ): String {
        val highlightId = id ?: ids.next("hl")
        edit {
            InvestigationRules.saveHighlight(
                it, highlightId, sourceType, sourceId, sectionId, questionId, text, note, now(),
            )
        }
        return highlightId
    }

    fun askQuestion(suspectId: String, question: Question) {
        val case = activeCase()
        edit { InvestigationRules.askQuestion(it, case, suspectId, question, now()) }
    }

    fun resetInterview(suspectId: String) {
        edit { InvestigationRules.resetInterview(it, suspectId) }
    }

    fun setSuspectNote(suspectId: String, note: String) {
        edit { InvestigationRules.setSuspectNote(it, suspectId, note) }
    }

    fun setItemNote(itemId: String, note: String) {
        edit { InvestigationRules.setItemNote(it, itemId, note) }
    }

    fun discoverItem(itemId: String) {
        edit { InvestigationRules.discoverItem(it, itemId) }
    }

    fun setScroll(fileId: String, offset: Double) {
        edit { InvestigationRules.setScroll(it, fileId, offset) }
    }

    fun pin(
        type: String,
        entityId: String,
        excerpt: String?,
        xPercent: Double? = null,
        yPercent: Double? = null,
    ): String {
        val existing = investigation().boardNodes.find { it.type == type && it.entityId == entityId }
        if (existing != null) return existing.id
        val id = ids.next("node")
        edit { InvestigationRules.pin(it, id, type, entityId, excerpt, xPercent, yPercent) }
        return id
    }

    fun moveNode(nodeId: String, x: Double, y: Double, persist: Boolean = true) {
        val id = state.progression.activeCaseId
        state = state.withInvestigation(id) { InvestigationRules.moveNode(it, nodeId, x, y) }
        if (persist) saves.saveCase(id, state.investigation(id))
    }

    fun setNodeExcerpt(nodeId: String, excerpt: String) {
        edit { InvestigationRules.setNodeExcerpt(it, nodeId, excerpt) }
    }

    fun setNodeNote(nodeId: String, note: String) {
        edit { InvestigationRules.setNodeNote(it, nodeId, note) }
    }

    fun removeNode(nodeId: String) {
        edit { InvestigationRules.removeNode(it, nodeId) }
    }

    fun connect(fromNodeId: String, toNodeId: String) {
        if (fromNodeId == toNodeId) return
        val already = investigation().boardConnections.any { connection ->
            setOf(connection.fromNodeId, connection.toNodeId) == setOf(fromNodeId, toNodeId)
        }
        if (already) return
        val id = ids.next("conn")
        edit { InvestigationRules.connect(it, id, fromNodeId, toNodeId) }
    }

    fun removeConnection(connectionId: String) {
        edit { InvestigationRules.removeConnection(it, connectionId) }
    }

    fun evaluate(accusedId: String, selectedNodeIds: Set<String>): Verdict {
        val nodes = investigation().boardNodes.filter { it.id in selectedNodeIds }
        val entityIds = nodes.map { it.entityId }.toSet()
        return evaluateIndictment(activeCase().solution, accusedId, entityIds)
    }

    /** Marks the case solved, clears its investigation, and unlocks the next file. */
    fun applySuccess(): String? {
        val currentId = state.progression.activeCaseId
        val next = CaseCatalog.nextId(currentId)
        val unlocked = state.progression.unlockedCaseIds.toMutableList()
        if (next != null && next !in unlocked) unlocked += next
        val progression = state.progression.copy(
            activeCaseId = next ?: currentId,
            unlockedCaseIds = unlocked,
            completedCaseIds = (state.progression.completedCaseIds + currentId).distinct(),
        )
        val cleared = CaseCatalog.freshState(CaseCatalog.require(currentId))
        var investigations = state.investigations + (currentId to cleared)
        if (next != null && next !in investigations) {
            investigations = investigations + (next to CaseCatalog.freshState(CaseCatalog.require(next)))
        }
        state = state.copy(progression = progression, investigations = investigations)
        saves.saveCase(currentId, cleared)
        saves.saveProgress(progression)
        return next
    }

    private fun edit(block: (CaseInvestigationState) -> CaseInvestigationState) {
        val id = state.progression.activeCaseId
        state = state.withInvestigation(id, block)
        saves.saveCase(id, state.investigation(id))
    }
}

private class IdFactory(private val now: () -> Long) {
    private var sequence = 0

    fun next(prefix: String): String {
        sequence += 1
        return "${prefix}_${now().toString(16)}_$sequence"
    }
}
