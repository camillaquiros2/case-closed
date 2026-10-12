package caseclosed

import caseclosed.data.CaseCatalog
import caseclosed.model.Verdict
import caseclosed.engine.evaluateIndictment
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
    fun yarnSagsBetweenPins() {
        val points = Yarn.sample(Vec(0.0, 10.0), Vec(200.0, 10.0))
        val mid = points[points.size / 2]
        assertTrue(mid.y > 20.0)
        val clamped = Yarn.clampPercent(96.0)
        assertEquals(85.0, clamped)
    }
}
