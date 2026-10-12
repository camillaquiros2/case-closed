package caseclosed.engine

import caseclosed.model.CaseSolution
import caseclosed.model.Verdict

/**
 * Pure indictment check. Evidence ids are entity ids of nodes currently
 * selected from the corkboard (item ids, suspect ids, or highlight ids).
 */
fun evaluateIndictment(
    solution: CaseSolution,
    accusedId: String,
    selectedEntityIds: Set<String>,
): Verdict {
    if (accusedId != solution.guiltySuspectId) {
        val critique = solution.failureHints[accusedId]
            ?: "The Chief Inspector slides the docket back across the desk."
        return Verdict.Rejected(critique)
    }
    val missing = solution.requiredEvidenceIds.filter { it !in selectedEntityIds }
    if (selectedEntityIds.size < solution.minEvidenceCount || missing.isNotEmpty()) {
        val critique = solution.failureHints[accusedId]
            ?: "The name is right. The pinned proof is not enough to hold."
        return Verdict.Rejected(critique)
    }
    return Verdict.Solved(solution.explanation)
}
