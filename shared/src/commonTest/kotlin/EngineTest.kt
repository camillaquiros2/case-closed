package caseclosed

import caseclosed.data.CaseCatalog
import caseclosed.engine.GameEngine
import caseclosed.engine.GameSaves
import caseclosed.model.CaseInvestigationState
import caseclosed.model.ClueKind
import caseclosed.model.GlobalProgression
import caseclosed.model.Verdict
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

    @Test
    fun clueLogRegistersEachDiscoveryOnceWithItsSource() {
        val saves = MemoryGameSaves()
        val engine = GameEngine(saves, now = { 50L })
        engine.load()
        engine.startCase()
        val case = engine.activeCase()

        val startingEvidence = engine.clueLog().filter { it.kind == ClueKind.EVIDENCE }.map { it.id }
        assertEquals(engine.investigation().discoveredItemIds, startingEvidence)

        assertTrue(engine.discoverItem("item_telegram"))
        engine.discoverItem("item_telegram")
        val telegram = engine.clueLog().single { it.id == "item_telegram" }
        assertEquals(false, telegram.relevant)

        val clue = assertNotNull(CaseCatalog.findClue(case, "hl_001"))
        repeat(2) {
            engine.saveHighlight("hl_001", "document", "file_001", "sec_001", null, clue.text, clue.description)
        }
        val excerpt = engine.clueLog().single { it.id == "hl_001" }
        assertEquals(ClueKind.DOCUMENT_EXCERPT, excerpt.kind)
        assertEquals(true, excerpt.relevant)
        assertEquals("file_001", excerpt.sourceId)
        assertEquals(case.files.first { it.id == "file_001" }.title, excerpt.title)

        val ownId = engine.saveHighlight(null, "document", "file_001", "sec_001", null, "La boca estaba seca.", "")
        assertNull(engine.clueLog().single { it.id == ownId }.relevant)

        val nodeId = assertNotNull(engine.addReference(excerpt))
        assertEquals(nodeId, engine.addReference(excerpt))
        assertTrue(engine.isReferenced(excerpt))
        assertEquals(1, engine.investigation().boardNodes.count { it.entityId == "hl_001" })
        assertNull(engine.addReference(excerpt.copy(id = "hl_004")))

        val reloaded = GameEngine(saves, now = { 50L })
        reloaded.load()
        assertEquals(engine.clueLog(), reloaded.clueLog())
    }

    @Test
    fun restartCaseClearsTheInvestigationButKeepsProgression() {
        val saves = MemoryGameSaves()
        val engine = GameEngine(saves, now = { 50L })
        engine.load()
        engine.startCase()
        val fresh = CaseCatalog.freshState(CaseCatalog.require("case_001"))

        engine.pin("item", "item_brandy", "The glass")
        engine.discoverItem("item_telegram")
        engine.setItemNote("item_brandy", "Huellas de Reid")
        val clue = assertNotNull(CaseCatalog.findClue(engine.activeCase(), "hl_001"))
        engine.saveHighlight("hl_001", "document", "file_001", "sec_001", null, clue.text, clue.description)

        engine.restartCase()
        assertEquals(fresh, engine.investigation())
        assertTrue(engine.isStarted())
        assertTrue(engine.clueLog().none { it.id == "hl_001" })

        val reloaded = GameEngine(saves, now = { 50L })
        reloaded.load()
        assertEquals(fresh, reloaded.investigation())

        engine.applySuccess()
        engine.startCase()
        engine.pin("item", engine.investigation().discoveredItemIds.first(), "First look")
        engine.restartCase()
        assertTrue(engine.isCompleted("case_001"))
        assertEquals("case_002", engine.progression.activeCaseId)
        assertTrue(engine.investigation().boardNodes.isEmpty())
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
