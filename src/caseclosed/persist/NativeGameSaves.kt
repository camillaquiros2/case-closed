package caseclosed.persist

import caseclosed.engine.GameSaves
import caseclosed.model.CaseInvestigationState
import caseclosed.model.GlobalProgression
import korlibs.korge.service.storage.NativeStorage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Platform key-value save: localStorage on JS, SharedPreferences on Android, a file on desktop.
 * Keys stay `cc.progression` and `cc.case.<id>` so existing desks keep loading.
 */
class NativeGameSaves(private val storage: NativeStorage) : GameSaves {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun loadProgress(): GlobalProgression? =
        storage.getOrNull(KEY_PROGRESS)?.let { raw ->
            runCatching { json.decodeFromString<GlobalProgression>(raw) }.getOrNull()
        }

    override fun saveProgress(progression: GlobalProgression) {
        storage[KEY_PROGRESS] = json.encodeToString(progression)
    }

    override fun loadCase(caseId: String): CaseInvestigationState? =
        storage.getOrNull(caseKey(caseId))?.let { raw ->
            runCatching { json.decodeFromString<CaseInvestigationState>(raw) }.getOrNull()
        }

    override fun saveCase(caseId: String, investigation: CaseInvestigationState) {
        storage[caseKey(caseId)] = json.encodeToString(investigation)
    }

    private fun caseKey(caseId: String) = "cc.case.$caseId"

    companion object {
        const val KEY_PROGRESS = "cc.progression"
    }
}
