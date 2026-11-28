package caseclosed.engine

import caseclosed.model.CaseData
import caseclosed.model.CaseFile
import caseclosed.model.CaseInvestigationState
import caseclosed.model.Item
import caseclosed.model.ObjectMemory
import caseclosed.model.Question
import caseclosed.model.ReferenceType

/**
 * Reads what the player is allowed to remember. It never invents a link or a conclusion.
 */
object MemoryQuery {
    const val LIMIT = 4

    fun memory(state: CaseInvestigationState, itemId: String): ObjectMemory =
        state.objectMemories[itemId] ?: ObjectMemory(itemId)

    fun reply(case: CaseData, state: CaseInvestigationState, suspectId: String, item: Item): String {
        val suspect = case.suspects.firstOrNull { it.id == suspectId } ?: return ignorance(item)
        val askable = suspect.questions.filter { question ->
            mentions(question, item) && InvestigationRules.canAskQuestion(state, case, suspectId, question)
        }
        val best = askable.firstOrNull { it.revealsItem == item.id } ?: askable.firstOrNull()
        return best?.answer ?: ignorance(item)
    }

    fun referenceName(case: CaseData, type: ReferenceType, id: String): String = when (type) {
        ReferenceType.PERSON -> case.suspects.firstOrNull { it.id == id }?.name ?: "Persona"
        ReferenceType.OBJECT -> case.items.firstOrNull { it.id == id }?.name ?: "Objeto"
        ReferenceType.DOCUMENT -> case.files.firstOrNull { it.id == id }?.title ?: "Expediente"
    }

    fun referenceImage(case: CaseData, type: ReferenceType, id: String): String? = when (type) {
        ReferenceType.PERSON -> case.suspects.firstOrNull { it.id == id }?.portrait
        ReferenceType.OBJECT -> case.items.firstOrNull { it.id == id }?.image
        ReferenceType.DOCUMENT -> case.files.firstOrNull { it.id == id }?.thumbnail
    }

    fun fileSentences(file: CaseFile): List<String> =
        file.sections.flatMap { section ->
            section.content.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        }.distinct()

    private fun mentions(question: Question, item: Item): Boolean {
        if (question.revealsItem == item.id) return true
        val blob = "${question.question} ${question.answer}".lowercase()
        return tokens(item).any { token -> token in blob }
    }

    private fun tokens(item: Item): List<String> =
        item.name.lowercase()
            .split(Regex("[^\\p{L}]+"))
            .filter { it.length >= 5 && it !in GENERIC }
            .sortedByDescending { it.length }
            .take(2)

    private fun ignorance(item: Item): String {
        val label = item.name.replaceFirstChar { it.lowercase() }
        return "No sabría decirle. No recuerdo nada sobre $label."
    }

    private val GENERIC = setOf("cristal", "escritorio", "veneno", "negro", "señor")
}
