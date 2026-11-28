package caseclosed

import caseclosed.data.CaseCatalog
import caseclosed.data.DeskImages
import caseclosed.data.TutorialSteps
import caseclosed.engine.GameEngine
import caseclosed.engine.GameSaves
import caseclosed.engine.MemoryQuery
import caseclosed.model.CaseInvestigationState
import caseclosed.model.ClueKind
import caseclosed.model.GlobalProgression
import caseclosed.model.HighlightRange
import caseclosed.model.ReferenceType
import caseclosed.model.Verdict
import kotlinx.serialization.json.Json
import caseclosed.ui.sentencesOf
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
        val phrase = "El Dr. Reid entró mientras yo salía, con su maletín negro, y dijo que solo tardaría un momento en tomarle el pulso a su señoría."
        assertTrue(engine.saveDocumentFragment("item_brandy", "file_003", phrase))
        assertTrue(
            engine.saveMentalNote("item_vial", null, ReferenceType.OBJECT, "item_vial", "Faltan dos dracmas en el frasco.", emptyList()),
        )
        val linked = engine.objectMemory("item_brandy").documents.single().id
        val noted = engine.objectMemory("item_vial").notes.single().id
        assertTrue(engine.evaluate("suspect_reid", setOf(linked, noted)) is Verdict.Solved)

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
    fun aMismatchedExtractDoesNotProveTheDoor() {
        val engine = GameEngine(MemoryGameSaves(), now = { 1L })
        engine.load()
        assertTrue(
            engine.saveDocumentFragment(
                "item_brandy",
                "file_001",
                "El cuerpo estaba desplomado sobre el secante. Una copa de cristal se encontraba al alcance de su mano derecha.",
            ),
        )
        assertTrue(
            engine.saveMentalNote("item_vial", null, ReferenceType.OBJECT, "item_vial", "Faltan dos dracmas en el frasco.", emptyList()),
        )
        val linked = engine.objectMemory("item_brandy").documents.single().id
        val noted = engine.objectMemory("item_vial").notes.single().id
        assertTrue(engine.evaluate("suspect_reid", setOf(linked, noted)) is Verdict.Rejected)
    }

    @Test
    fun labExtractsFiledOnTheVialCloseTheCaseWithGraves() {
        val engine = GameEngine(MemoryGameSaves(), now = { 1L })
        engine.load()
        val morris = engine.activeCase().suspects.first { it.id == "suspect_morris" }
        engine.askQuestion("suspect_morris", morris.questions.first { it.id == "q_m2" })
        assertTrue(engine.fileTestimony("suspect_morris", "q_m2"))
        assertTrue(
            engine.saveDocumentFragment(
                "item_vial",
                "file_002",
                "El residuo blanco en el interior de la copa es belladona (veneno) concentrada.",
            ),
        )
        assertTrue(
            engine.saveDocumentFragment(
                "item_vial",
                "file_002",
                "Las huellas en el borde de la copa coinciden con las del Dr. Alistair Reid.",
            ),
        )
        val extracts = engine.objectMemory("item_vial").documents.map { it.id }
        val testimony = engine.investigation().filedTestimonies.single().id
        assertTrue(engine.evaluate("suspect_reid", (extracts + testimony).toSet()) is Verdict.Solved)
    }

    @Test
    fun aFiledAnswerCountsAsItsClue() {
        val engine = GameEngine(MemoryGameSaves(), now = { 1L })
        engine.load()
        val morris = engine.activeCase().suspects.first { it.id == "suspect_morris" }
        val question = morris.questions.first { it.id == "q_m2" }
        engine.askQuestion("suspect_morris", question)
        assertTrue(engine.fileTestimony("suspect_morris", "q_m2"))
        assertTrue(
            engine.saveMentalNote("item_brandy", null, ReferenceType.PERSON, "suspect_reid", "Reid estuvo allí con el maletín negro.", emptyList()),
        )
        assertTrue(
            engine.saveMentalNote("item_brandy", null, ReferenceType.OBJECT, "item_brandy", "La copa de brandy tiene el residuo.", emptyList()),
        )
        assertTrue(
            engine.saveMentalNote("item_vial", null, ReferenceType.OBJECT, "item_vial", "Faltan dos dracmas en el frasco.", emptyList()),
        )
        val testimony = engine.investigation().filedTestimonies.single().id
        val brandy = engine.objectMemory("item_brandy").notes.first { it.referenceType == ReferenceType.OBJECT }.id
        val vial = engine.objectMemory("item_vial").notes.single().id
        assertTrue(engine.evaluate("suspect_reid", setOf(testimony, brandy, vial)) is Verdict.Solved)
    }

    @Test
    fun linkedGravesPhraseCountsAsTheStudyClue() {
        val engine = GameEngine(MemoryGameSaves(), now = { 1L })
        engine.load()
        val phrase = "El Dr. Reid entró mientras yo salía, con su maletín negro, y dijo que solo tardaría un momento en tomarle el pulso a su señoría."
        assertTrue(engine.saveDocumentFragment("item_brandy", "file_003", phrase))
        assertTrue(
            engine.saveMentalNote("item_brandy", null, ReferenceType.OBJECT, "item_vial", "El frasco del maletín está incompleto.", emptyList()),
        )
        val linked = engine.objectMemory("item_brandy").documents.single().id
        val noted = engine.objectMemory("item_brandy").notes.single().id
        assertTrue(engine.evaluate("suspect_reid", setOf(linked, noted)) is Verdict.Solved)
    }

    @Test
    fun objectReplyUsesTheScriptThePlayerCanAlreadyHear() {
        val engine = GameEngine(MemoryGameSaves(), now = { 1L })
        engine.load()
        val case = engine.activeCase()
        val brandy = case.items.first { it.id == "item_brandy" }
        val morris = MemoryQuery.reply(case, engine.investigation(), "suspect_morris", brandy)
        assertTrue(morris.contains("serví", ignoreCase = true))
        val eleanor = MemoryQuery.reply(case, engine.investigation(), "suspect_eleanor", brandy)
        assertTrue(eleanor.startsWith("No sabría"))
        val lamp = case.items.first { it.id == "item_lamp" }
        val aboutLamp = MemoryQuery.reply(case, engine.investigation(), "suspect_reid", lamp)
        assertTrue(aboutLamp.startsWith("No sabría"))
    }

    @Test
    fun memoryStaysOnEachObjectAndReloads() {
        val saves = MemoryGameSaves()
        val engine = GameEngine(saves, now = { 1L })
        engine.load()
        repeat(5) { index ->
            val saved = engine.saveDocumentFragment("item_brandy", "file_001", "Frase $index del brandy.")
            if (index < 4) assertTrue(saved) else assertFalse(saved)
        }
        assertEquals(4, engine.objectMemory("item_brandy").documents.size)
        assertTrue(engine.objectMemory("item_pillbox").documents.isEmpty())
        val first = engine.objectMemory("item_brandy").documents.first().id
        engine.removeDocumentFragment("item_brandy", first)
        assertEquals(3, engine.objectMemory("item_brandy").documents.size)
        assertTrue(
            engine.saveMentalNote(
                "item_brandy",
                null,
                ReferenceType.PERSON,
                "suspect_morris",
                "Morris dejó la bandeja.",
                listOf(HighlightRange(0, 6)),
            ),
        )
        assertTrue(
            engine.saveMentalNote(
                "item_brandy",
                null,
                ReferenceType.OBJECT,
                "item_vial",
                "Alguna nota importante",
                listOf(HighlightRange(7, 11)),
            ),
        )
        assertFalse(engine.saveMentalNote("item_brandy", null, ReferenceType.OBJECT, "missing", "Nada", emptyList()))
        val reloaded = GameEngine(saves, now = { 2L })
        reloaded.load()
        val notes = reloaded.objectMemory("item_brandy").notes
        val person = notes.first { it.referenceType == ReferenceType.PERSON }
        assertEquals("suspect_morris", person.referenceId)
        assertEquals("Morris dejó la bandeja.", person.text)
        val note = notes.first { it.referenceType == ReferenceType.OBJECT }
        assertEquals("Alguna nota importante", note.text)
        assertEquals(ReferenceType.OBJECT, note.referenceType)
        assertEquals(7, note.highlights.single().start)
        assertEquals(11, note.highlights.single().endExclusive)
        assertEquals(3, reloaded.objectMemory("item_brandy").documents.size)
    }

    @Test
    fun oldInvestigationJsonOmitsMemoryAndStillLoads() {
        val json = Json { ignoreUnknownKeys = true }
        val state = json.decodeFromString<CaseInvestigationState>("""{"discoveredItemIds":["item_brandy"]}""")
        assertEquals(listOf("item_brandy"), state.discoveredItemIds)
        assertTrue(state.objectMemories.isEmpty())
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

    @Test
    fun mainMenuCoverIsLoadedAndEveryCaseHasAnIntro() {
        assertTrue(DeskImages.MENU_BACKGROUND in CaseCatalog.imagePaths())
        for (pack in CaseCatalog.packs) {
            assertTrue(
                sentencesOf(pack.data.briefing).isNotEmpty(),
                "${pack.data.id} needs a briefing for its intro",
            )
        }
    }

    @Test
    fun tutorialIsOfferedOnceAndLeavesTheCaseAlone() {
        val saves = MemoryGameSaves()
        val engine = GameEngine(saves, now = { 50L })
        engine.load()
        assertFalse(engine.isTutorialSeen())

        engine.startCase()
        engine.pin("item", "item_vial", "The vial")
        val before = engine.investigation()

        engine.markTutorialSeen()
        engine.markTutorialSeen()

        assertTrue(engine.isTutorialSeen())
        assertTrue(engine.isStarted())
        assertEquals(before, engine.investigation())

        val reloaded = GameEngine(saves, now = { 50L })
        reloaded.load()
        assertTrue(reloaded.isTutorialSeen())
        assertEquals(before, reloaded.investigation())
    }

    @Test
    fun everyTutorialStepFitsOnScreenAndItsImageIsLoaded() {
        val loaded = CaseCatalog.imagePaths()
        assertTrue(TutorialSteps.all.isNotEmpty())
        for (step in TutorialSteps.all) {
            assertTrue(step.title.isNotBlank())
            assertTrue(step.body.length <= TutorialSteps.MAX_BODY, "\"${step.title}\" is too long to fit without scrolling")
            step.images.forEach { assertTrue(it in loaded, "$it is not preloaded") }
        }
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
