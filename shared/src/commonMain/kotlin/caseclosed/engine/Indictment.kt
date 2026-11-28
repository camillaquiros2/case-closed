package caseclosed.engine

import caseclosed.model.CaseData
import caseclosed.model.CaseInvestigationState
import caseclosed.model.CaseSolution
import caseclosed.model.ReferenceType
import caseclosed.model.Verdict

/**
 * Ids a filed note, linked expediente or saved answer stands for.
 * An extract counts as a clue only when it carries that clue's keywords.
 */
fun memoryEvidenceIds(
    case: CaseData,
    state: CaseInvestigationState,
    selectedIds: Set<String>,
): Set<String> {
    if (selectedIds.isEmpty()) return emptySet()
    val clues = mutableListOf<Triple<String, String, List<String>>>()
    for (file in case.files) {
        for (section in file.sections) {
            val cited = section.references.orEmpty().map { it.targetId }
            for (clue in section.highlightableClues.orEmpty()) {
                clues += Triple(clue.id, clue.text, cited)
            }
        }
    }
    val ids = mutableSetOf<String>()
    fun absorb(text: String) {
        for ((id, clue, cited) in clues) {
            if (!containsClueKeywords(text, clue)) continue
            ids += id
            ids += cited
        }
    }
    for ((objectId, memory) in state.objectMemories) {
        for (document in memory.documents) {
            if (document.id !in selectedIds) continue
            ids += objectId
            absorb(document.fragment)
        }
        for (note in memory.notes) {
            if (note.referenceType == ReferenceType.PERSON) continue
            if (note.id !in selectedIds) continue
            ids += objectId
            ids += note.referenceId
            absorb(note.text)
        }
    }
    for (filed in state.filedTestimonies) {
        if (filed.id !in selectedIds) continue
        val question = case.suspects.firstOrNull { it.id == filed.suspectId }
            ?.questions
            ?.firstOrNull { it.id == filed.questionId }
            ?: continue
        question.revealsClue?.let { ids += it }
        question.revealsItem?.let { ids += it }
        absorb(question.answer)
    }
    return ids
}

private val CLUE_STOPWORDS = setOf(
    "para", "como", "pero", "esta", "este", "esto", "esa", "ese", "eso",
    "solo", "mas", "muy", "entre", "sobre", "desde", "hasta", "cuando",
    "donde", "porque", "tambien", "hacia", "tiene", "estan", "estaba",
    "usted", "ella", "ellos", "ellas", "unos", "unas",
)

/** True when [saved] includes every content word of the expected extract. */
fun containsClueKeywords(saved: String, clue: String): Boolean {
    val expected = keywords(clue)
    if (expected.isEmpty()) return false
    val found = keywords(saved)
    return expected.all { it in found }
}

private fun keywords(text: String): Set<String> {
    val words = mutableSetOf<String>()
    val current = StringBuilder()
    fun flush() {
        if (current.length >= 4) {
            val word = foldWord(current.toString())
            if (word.length >= 4 && word !in CLUE_STOPWORDS) words += word
        }
        current.clear()
    }
    for (ch in text.lowercase()) {
        if (ch.isLetter()) current.append(ch) else flush()
    }
    flush()
    return words
}

private fun foldWord(word: String): String = buildString(word.length) {
    for (ch in word) {
        append(
            when (ch) {
                'á' -> 'a'
                'é' -> 'e'
                'í' -> 'i'
                'ó' -> 'o'
                'ú', 'ü' -> 'u'
                'ñ' -> 'n'
                else -> ch
            },
        )
    }
}

/**
 * Pure indictment check. [selectedEntityIds] are the objects, people, files
 * and clues carried by the notes and linked expedientes the player filed.
 */
fun evaluateIndictment(
    solution: CaseSolution,
    accusedId: String,
    selectedEntityIds: Set<String>,
): Verdict {
    if (accusedId != solution.guiltySuspectId) {
        val critique = solution.failureHints[accusedId]
            ?: "El inspector jefe desliza el expediente de vuelta por el escritorio."
        return Verdict.Rejected(critique)
    }
    val missing = solution.requiredEvidenceIds.filter { it !in selectedEntityIds }
    if (selectedEntityIds.size < solution.minEvidenceCount || missing.isNotEmpty()) {
        val critique = solution.failureHints[accusedId]
            ?: "El nombre es correcto. Las pruebas fijadas no bastan para sostenerlo."
        return Verdict.Rejected(critique)
    }
    return Verdict.Solved(solution.explanation)
}
