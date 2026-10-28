package caseclosed.engine

import caseclosed.model.CaseInvestigationState
import caseclosed.model.GlobalProgression

/** Load and store progression and per-case investigations. No game rules. */
interface GameSaves {
    fun loadProgress(): GlobalProgression?
    fun saveProgress(progression: GlobalProgression)
    fun loadCase(caseId: String): CaseInvestigationState?
    fun saveCase(caseId: String, investigation: CaseInvestigationState)
}
