package caseclosed

import caseclosed.data.CaseCatalog
import caseclosed.dev.SceneGuides
import caseclosed.engine.evaluateIndictment
import caseclosed.model.ItemOrigin
import caseclosed.model.Verdict
import caseclosed.ui.Vec
import caseclosed.ui.Yarn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogicTest {
    @Test
    fun catalogIsConsistent() {
        CaseCatalog.validate()
        assertEquals(listOf("case_001", "case_002"), CaseCatalog.packs.map { it.data.id })
    }

    @Test
    fun wrongSuspectIsRejectedAndBoardWouldRemain() {
        val solution = CaseCatalog.require("case_001").data.solution
        val verdict = evaluateIndictment(
            solution,
            "suspect_julian",
            setOf("item_brandy", "item_vial", "hl_003"),
        )
        assertTrue(verdict is Verdict.Rejected)
    }

    @Test
    fun guiltyNameStillNeedsThePinnedProof() {
        val solution = CaseCatalog.require("case_001").data.solution
        val verdict = evaluateIndictment(solution, "suspect_reid", setOf("item_brandy"))
        assertTrue(verdict is Verdict.Rejected)
    }

    @Test
    fun blackwoodSolvesWhenTheThreePinsAreFiled() {
        val solution = CaseCatalog.require("case_001").data.solution
        val verdict = evaluateIndictment(
            solution,
            "suspect_reid",
            setOf("item_brandy", "item_vial", "hl_003"),
        )
        assertTrue(verdict is Verdict.Solved)
    }

    @Test
    fun pierSolvesOnLedgerKeyAndAlibi() {
        val solution = CaseCatalog.require("case_002").data.solution
        val verdict = evaluateIndictment(
            solution,
            "suspect_silas",
            setOf("item_ledger", "item_key", "hl_202"),
        )
        assertTrue(verdict is Verdict.Solved)
    }

    @Test
    fun aNewCaseStartsWithOnlyTheFileEvidence() {
        val blackwood = CaseCatalog.freshState(CaseCatalog.require("case_001"))
        assertEquals(listOf("item_brandy", "item_pillbox"), blackwood.discoveredItemIds)
        val lamp = CaseCatalog.require("case_001").data.items.first { it.id == "item_lamp" }
        assertEquals(ItemOrigin.SCENE, lamp.origin)
        assertTrue(!lamp.relevant)
        val pier = CaseCatalog.freshState(CaseCatalog.require("case_002"))
        assertEquals(listOf("item_ledger"), pier.discoveredItemIds)
    }

    @Test
    fun layoutGuidePaintsTheFirstPartOfTheRoom() {
        val case = CaseCatalog.require("case_001").data
        val scene = case.scenes.first()
        val box = scene.subscenes.first().bounds
        val bmp = SceneGuides.render(case, scene)
        val x = SceneGuides.MAP_X + ((box.x + box.w / 2.0) / 100.0 * SceneGuides.MAP_W).toInt()
        val y = SceneGuides.MAP_Y + ((box.y + box.h / 2.0) / 100.0 * SceneGuides.MAP_H).toInt()
        val painted = bmp[x, y]
        val expected = SceneGuides.regionColor(0)
        assertEquals(expected.r, painted.r)
        assertEquals(expected.g, painted.g)
        assertEquals(expected.b, painted.b)
    }

    @Test
    fun yarnSagsBetweenPins() {
        val points = Yarn.sample(Vec(0.0, 10.0), Vec(200.0, 10.0))
        val mid = points[points.size / 2]
        assertTrue(mid.y > 20.0)
        val clamped = Yarn.clampPercent(96.0)
        assertEquals(85.0, clamped)
    }
}
