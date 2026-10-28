package caseclosed.data

import caseclosed.model.CaseData
import caseclosed.model.CaseInvestigationState
import caseclosed.model.CasePack
import caseclosed.model.ClueDef
import caseclosed.model.ItemOrigin
import caseclosed.model.PercentBox

/**
 * Every playable case. Add a new file, then append the pack here.
 * The desk, board, and indictment screens only read this list.
 */
object CaseCatalog {
    val packs: List<CasePack> = listOf(case001, case002)

    fun require(id: String): CasePack =
        packs.firstOrNull { it.data.id == id } ?: error("Unknown case: $id")

    fun nextId(id: String): String? {
        val index = packs.indexOfFirst { it.data.id == id }
        if (index < 0) return null
        return packs.getOrNull(index + 1)?.data?.id
    }

    fun freshState(pack: CasePack): CaseInvestigationState =
        CaseInvestigationState(
            discoveredItemIds = pack.data.items.filter { it.origin == ItemOrigin.FILE }.map { it.id },
        )

    fun findClue(case: CaseData, clueId: String): ClueDef? {
        for (file in case.files) {
            for (section in file.sections) {
                val clue = section.highlightableClues?.firstOrNull { it.id == clueId }
                if (clue != null) return clue
            }
        }
        return null
    }

    fun imagePaths(): List<String> {
        val paths = linkedSetOf(
            "images/desk/desk_wood_bg.png",
            "images/desk/cork_texture.png",
        )
        for (pack in packs) {
            val case = pack.data
            for (file in case.files) {
                if (file.thumbnail.isNotBlank()) paths += file.thumbnail
                for (section in file.sections) {
                    section.image?.src?.let { if (it.isNotBlank()) paths += it }
                }
            }
            for (suspect in case.suspects) if (suspect.portrait.isNotBlank()) paths += suspect.portrait
            for (item in case.items) if (item.image.isNotBlank()) paths += item.image
            for (scene in case.scenes) {
                if (scene.image.isNotBlank()) paths += scene.image
                for (sub in scene.subscenes) if (sub.image.isNotBlank()) paths += sub.image
            }
        }
        return paths.toList()
    }

    fun validate() {
        val seen = mutableSetOf<String>()
        for (pack in packs) {
            val case = pack.data
            require(case.id !in seen) { "Duplicate case id ${case.id}" }
            seen += case.id
            val suspects = case.suspects.map { it.id }.toSet()
            val items = case.items.map { it.id }.toSet()
            val files = case.files.map { it.id }.toSet()
            require(case.solution.guiltySuspectId in suspects) { "${case.id} guilty id missing" }
            require(case.solution.requiredEvidenceIds.size >= case.solution.minEvidenceCount) {
                "${case.id} asks for more evidence than it defines"
            }
            val clueIds = case.files.flatMap { it.sections }.flatMap { it.highlightableClues.orEmpty() }.map { it.id }.toSet()
            for (id in case.solution.requiredEvidenceIds) {
                require(id in items || id in suspects || id in clueIds) {
                    "${case.id} required evidence $id is not an item, suspect, or clue"
                }
            }
            for (suspectId in suspects) {
                require(suspectId in case.solution.failureHints) { "${case.id} missing failure hint for $suspectId" }
            }
            for (suspect in case.suspects) {
                val words = suspect.shortDescription.trim().split(Regex("\\s+")).count { it.isNotBlank() }
                require(words <= 40) { "${suspect.id} shortDescription is $words words" }
                for (question in suspect.questions) {
                    question.revealsItem?.let { require(it in items) { "${question.id} reveals unknown item $it" } }
                    question.revealsClue?.let { require(it in clueIds) { "${question.id} reveals unknown clue $it" } }
                }
            }
            for (item in case.items) {
                if (item.id in case.solution.requiredEvidenceIds) {
                    require(item.relevant) { "${case.id} requires irrelevant item ${item.id}" }
                }
            }
            val clues = case.files.flatMap { it.sections }.flatMap { it.highlightableClues.orEmpty() }
            for (clue in clues) {
                if (clue.id in case.solution.requiredEvidenceIds) {
                    require(clue.relevant) { "${case.id} requires irrelevant clue ${clue.id}" }
                }
            }
            validateScenes(case, items)
            for (file in case.files) {
                for (id in file.relatedSuspectIds) require(id in suspects)
                for (id in file.relatedItemIds) require(id in items)
                for (section in file.sections) {
                    for (ref in section.references.orEmpty()) {
                        val ok = when (ref.type) {
                            "suspect" -> ref.targetId in suspects
                            "item" -> ref.targetId in items
                            "file" -> ref.targetId in files
                            else -> false
                        }
                        require(ok) { "${section.id} bad reference ${ref.type}:${ref.targetId}" }
                    }
                }
            }
            for (item in case.items) {
                for (id in item.relatedFileIds) require(id in files) { "${item.id} related file $id missing" }
            }
        }
    }

    private fun validateScenes(case: CaseData, items: Set<String>) {
        val sceneIds = mutableSetOf<String>()
        val placed = mutableSetOf<String>()
        val idPattern = Regex("[a-z0-9_]+")
        for (scene in case.scenes) {
            require(scene.id.matches(idPattern)) { "${case.id} scene id ${scene.id}" }
            require(scene.id !in sceneIds) { "${case.id} duplicate scene ${scene.id}" }
            sceneIds += scene.id
            val overhead = "images/cases/${case.id}/scenes/${scene.id}/overhead.png"
            require(scene.image == overhead) { "${scene.id} overhead must be $overhead" }
            val subIds = mutableSetOf<String>()
            for (sub in scene.subscenes) {
                require(sub.id.matches(idPattern)) { "${scene.id} subscene id ${sub.id}" }
                require(sub.id !in subIds) { "${scene.id} duplicate subscene ${sub.id}" }
                subIds += sub.id
                val closeup = "images/cases/${case.id}/scenes/${scene.id}/${sub.id}.png"
                require(sub.image == closeup) { "${sub.id} image must be $closeup" }
                requireBox(sub.bounds, "${scene.id}/${sub.id}")
                for (spot in sub.items) {
                    require(spot.itemId in items) { "${sub.id} places unknown ${spot.itemId}" }
                    require(spot.itemId !in placed) { "${spot.itemId} is placed twice" }
                    placed += spot.itemId
                    requireBox(spot.bounds, spot.itemId)
                }
            }
        }
        for (item in case.items) {
            if (item.origin == ItemOrigin.SCENE) {
                require(item.id in placed) { "${item.id} is a scene item with no spot" }
            }
        }
    }

    private fun requireBox(box: PercentBox, label: String) {
        require(box.w > 0.0 && box.h > 0.0) { "$label has no area" }
        require(box.x >= 0.0 && box.y >= 0.0 && box.x + box.w <= 100.01 && box.y + box.h <= 100.01) {
            "$label sits outside the picture"
        }
    }
}
