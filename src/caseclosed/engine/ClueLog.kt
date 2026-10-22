package caseclosed.engine

import caseclosed.data.CaseCatalog
import caseclosed.model.CaseData
import caseclosed.model.CaseInvestigationState
import caseclosed.model.ClueEntry
import caseclosed.model.ClueKind
import caseclosed.model.HighlightEntry
import caseclosed.model.Item
import caseclosed.model.ItemOrigin

/** Builds the clue log from what the detective has found. Pure reads, no edits. */
internal object ClueLog {
    private const val INTERVIEW = "interview"
    private const val SEPARATOR = "  ·  "

    fun entries(current: CaseInvestigationState, case: CaseData): List<ClueEntry> {
        val evidence = current.discoveredItemIds.distinct().mapNotNull { id ->
            case.items.firstOrNull { it.id == id }?.let { evidenceEntry(current, case, it) }
        }
        val quotes = current.highlights
            .distinctBy { it.id }
            .sortedBy { it.createdAt }
            .map { quoteEntry(case, it) }
        return evidence + quotes
    }

    private fun evidenceEntry(current: CaseInvestigationState, case: CaseData, item: Item) = ClueEntry(
        id = item.id,
        kind = ClueKind.EVIDENCE,
        title = item.name,
        text = item.shortNotes,
        note = current.itemNotes[item.id].orEmpty(),
        sourceId = item.id,
        origin = itemOrigin(case, item),
        relevant = item.relevant,
    )

    private fun quoteEntry(case: CaseData, highlight: HighlightEntry): ClueEntry {
        val testimony = highlight.sourceType == INTERVIEW
        val (title, origin) = if (testimony) testimonySource(case, highlight) else documentSource(case, highlight)
        return ClueEntry(
            id = highlight.id,
            kind = if (testimony) ClueKind.TESTIMONY else ClueKind.DOCUMENT_EXCERPT,
            title = title,
            text = highlight.text,
            note = highlight.note,
            sourceId = highlight.sourceId,
            origin = origin,
            relevant = CaseCatalog.findClue(case, highlight.id)?.relevant,
        )
    }

    private fun testimonySource(case: CaseData, highlight: HighlightEntry): Pair<String, String> {
        val suspect = case.suspects.firstOrNull { it.id == highlight.sourceId }
        val question = suspect?.questions?.firstOrNull { it.id == highlight.questionId }
        return (suspect?.name ?: highlight.sourceId) to (question?.question ?: suspect?.alias.orEmpty())
    }

    private fun documentSource(case: CaseData, highlight: HighlightEntry): Pair<String, String> {
        val file = case.files.firstOrNull { it.id == highlight.sourceId }
        val section = file?.sections?.firstOrNull { it.id == highlight.sectionId }
        val origin = listOfNotNull(file?.documentNumber, section?.heading).joinToString(SEPARATOR)
        return (file?.title ?: highlight.sourceId) to origin
    }

    /** Where the item came from: its scene spot, the file that logged it, or the suspect who revealed it. */
    private fun itemOrigin(case: CaseData, item: Item): String {
        val fromScene = case.scenes.firstNotNullOfOrNull { scene ->
            scene.subscenes
                .firstOrNull { sub -> sub.items.any { it.itemId == item.id } }
                ?.let { "${scene.name}$SEPARATOR${it.name}" }
        }
        val fromFile = case.files
            .firstOrNull { it.id in item.relatedFileIds || item.id in it.relatedItemIds }
            ?.title
        val fromInterview = case.suspects
            .firstOrNull { suspect -> suspect.questions.any { it.revealsItem == item.id } }
            ?.name
        return when (item.origin) {
            ItemOrigin.SCENE -> fromScene ?: fromInterview ?: fromFile
            ItemOrigin.FILE -> fromFile ?: fromInterview ?: fromScene
        } ?: item.name
    }
}
