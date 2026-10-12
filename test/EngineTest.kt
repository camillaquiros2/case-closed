package caseclosed

import caseclosed.data.CaseCatalog
import caseclosed.engine.GameEngine
import caseclosed.engine.GameSaves
import caseclosed.model.CaseInvestigationState
import caseclosed.model.GlobalProgression
import caseclosed.model.Verdict
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EngineTest {
    @Test
    fun pinConnectAndSolveStayInStateAndReloadFromSaves() {
        val saves = MemoryGameSaves()
        val engine = GameEngine(saves, now = { 50L })
        engine.load()

        val brandy = engine.pin("item", "item_brandy", "The glass")
        val vial = engine.pin("item", "item_vial", "The vial")
        val quote = engine.pin("highlight", "hl_003", "The alibi")
        assertEquals(brandy, engine.pin("item", "item_brandy", "again"))
        engine.connect(brandy, vial)
        engine.connect(vial, quote)
        engine.connect(brandy, vial)

        assertEquals(3, engine.investigation().boardNodes.size)
        assertEquals(2, engine.investigation().boardConnections.size)
        assertTrue(engine.evaluate("suspect_reid", setOf(brandy, vial, quote)) is Verdict.Solved)

        val reloaded = GameEngine(saves, now = { 50L })
        reloaded.load()
        assertEquals(3, reloaded.investigation().boardNodes.size)
        assertEquals(2, reloaded.investigation().boardConnections.size)

        val next = reloaded.applySuccess()
        assertEquals("case_002", next)
        assertTrue(reloaded.isCompleted("case_001"))
        assertEquals(0, reloaded.state.investigation("case_001").boardNodes.size)
        assertEquals("case_002", reloaded.progression.activeCaseId)
    }

    @Test
    fun startCaseBeginsCleanAndKeepsProgressOnReturn() {
        val saves = MemoryGameSaves()
        val engine = GameEngine(saves, now = { 50L })
        engine.load()
        val caseId = engine.progression.activeCaseId
        val authored = CaseCatalog.require(caseId).initialDiscoveredItemIds

        assertFalse(engine.isStarted())
        engine.startCase()
        assertTrue(engine.isStarted())
        assertEquals(authored, engine.investigation().discoveredItemIds)
        assertTrue(engine.investigation().boardNodes.isEmpty())
        assertTrue(engine.investigation().highlights.isEmpty())
        assertTrue(engine.investigation().interviews.isEmpty())

        engine.pin("item", authored.first(), "First look")
        engine.startCase()
        assertEquals(1, engine.investigation().boardNodes.size)

        val reloaded = GameEngine(saves, now = { 50L })
        reloaded.load()
        assertTrue(reloaded.isStarted())
        reloaded.startCase()
        assertEquals(1, reloaded.investigation().boardNodes.size)
    }
}

private class MemoryGameSaves : GameSaves {
    var progress: GlobalProgression? = null
    val cases = mutableMapOf<String, CaseInvestigationState>()

    override fun loadProgress(): GlobalProgression? = progress

    override fun saveProgress(progression: GlobalProgression) {
        progress = progression
    }

    override fun loadCase(caseId: String): CaseInvestigationState? = cases[caseId]

    override fun saveCase(caseId: String, investigation: CaseInvestigationState) {
        cases[caseId] = investigation
    }
}
