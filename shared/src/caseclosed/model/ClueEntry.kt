package caseclosed.model

/** Kind of clue in the log. The board stores evidence and quotes under these types. */
enum class ClueKind(val boardType: String) {
    EVIDENCE("item"),
    DOCUMENT_EXCERPT("highlight"),
    TESTIMONY("highlight"),
}

/**
 * One line of the clue log. Built from the saved investigation and never stored,
 * so it always matches what the detective actually found.
 */
data class ClueEntry(
    /** Item id or highlight id. Same id the evidence board pins. */
    val id: String,
    val kind: ClueKind,
    val title: String,
    val text: String,
    val note: String,
    /** Item, file, or suspect that holds the original content. */
    val sourceId: String,
    /** Readable origin, for example "CC-1924-014-A  ·  Examen". */
    val origin: String,
    /** Authored relevance. Null when the case does not classify it, like a sentence the player marked. */
    val relevant: Boolean?,
)
