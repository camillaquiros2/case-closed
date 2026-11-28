package caseclosed.engine

import caseclosed.data.CaseCatalog
import caseclosed.model.BoardConnection
import caseclosed.model.BoardNode
import caseclosed.model.CaseData
import caseclosed.model.CaseInvestigationState
import caseclosed.model.DocumentConnection
import caseclosed.model.FiledTestimony
import caseclosed.model.HighlightEntry
import caseclosed.model.HighlightRange
import caseclosed.model.InterviewState
import caseclosed.model.InterviewTurn
import caseclosed.model.MentalNote
import caseclosed.model.ObjectMemory
import caseclosed.model.Question
import caseclosed.model.ReferenceType

/** Pure edits to one case investigation. The engine supplies ids and timestamps. */
internal object InvestigationRules {
    // Consulta los descubrimientos obtenidos
    fun discoveredIds(
        current: CaseInvestigationState,
        case: CaseData,
    ): Set<String> {
        val possibleIds = case.discoveries.map { it.id }.toSet()

        return (
            current.discoveredItemIds +
                current.highlights.map { it.id }
            ).filter { it in possibleIds }.toSet()
    }

    // Comprueba los requisitos de una pista
    fun canDiscover(
        current: CaseInvestigationState,
        case: CaseData,
        id: String,
    ): Boolean {
        val discovery = case.discoveries
            .firstOrNull { it.id == id }
            ?: return false

        val found = discoveredIds(current, case)

        return id in found ||
            discovery.requiredDiscoveryIds.all { it in found }
    }
    // Comprueba si la pregunta pertenece al sospechoso y cumple los requisitos
    fun canAskQuestion(
        current: CaseInvestigationState,
        case: CaseData,
        suspectId: String,
        question: Question,
    ): Boolean {
        val authoredQuestion = case.suspects
            .firstOrNull { it.id == suspectId }
            ?.questions
            ?.firstOrNull { it.id == question.id }
            ?: return false

        if (authoredQuestion != question) return false

        val found = discoveredIds(current, case)

        val requirementsMet = question.requiredDiscoveryIds
            .all { it in found }

        val rewardsAvailable = listOfNotNull(
            question.revealsItem,
            question.revealsClue,
        ).all { canDiscover(current, case, it) }

        return requirementsMet && rewardsAvailable
    }

    fun markMessagesRead(current: CaseInvestigationState, messageIds: List<String>): CaseInvestigationState =
        current.copy(readMessageIds = (current.readMessageIds + messageIds).distinct())

    fun saveHighlight(
        current: CaseInvestigationState,
        highlightId: String,
        sourceType: String,
        sourceId: String,
        sectionId: String?,
        questionId: String?,
        text: String,
        note: String,
        now: Long,
    ): CaseInvestigationState {
        val existing = current.highlights.find { it.id == highlightId }
        val entry = HighlightEntry(
            id = highlightId,
            sourceType = sourceType,
            sourceId = sourceId,
            sectionId = sectionId,
            questionId = questionId,
            text = text,
            note = note,
            createdAt = existing?.createdAt ?: now,
        )
        val highlights = if (existing == null) {
            current.highlights + entry
        } else {
            current.highlights.map { if (it.id == highlightId) entry else it }
        }
        return current.copy(highlights = highlights)
    }

    fun askQuestion(
        current: CaseInvestigationState,
        case: CaseData,
        suspectId: String,
        question: Question,
        now: Long,
    ): CaseInvestigationState {
        if (!canAskQuestion(current, case, suspectId, question)) {
            return current
        }
        val prev = current.interviews[suspectId] ?: InterviewState()
        if (question.id in prev.askedQuestionIds) return current
        val interview = prev.copy(
            askedQuestionIds = prev.askedQuestionIds + question.id,
            conversationOrder = prev.conversationOrder + InterviewTurn(question.id, now),
            isSaved = true,
        )
        var discovered = current.discoveredItemIds
        question.revealsItem?.let { itemId ->
            if (itemId !in discovered) discovered = discovered + itemId
        }
        var highlights = current.highlights
        question.revealsClue?.let { clueId ->
            if (highlights.none { it.id == clueId }) {
                val clue = CaseCatalog.findClue(case, clueId)
                highlights = highlights + HighlightEntry(
                    id = clueId,
                    sourceType = "interview",
                    sourceId = suspectId,
                    sectionId = null,
                    questionId = question.id,
                    text = clue?.text ?: question.answer,
                    note = clue?.description ?: "",
                    createdAt = now,
                )
            }
        }
        return current.copy(
            interviews = current.interviews + (suspectId to interview),
            discoveredItemIds = discovered,
            highlights = highlights,
        )
    }

    fun resetInterview(current: CaseInvestigationState, suspectId: String): CaseInvestigationState {
        val filed = current.filedTestimonies.filter { it.suspectId != suspectId }
        if (suspectId !in current.interviews && filed.size == current.filedTestimonies.size) return current
        return current.copy(
            interviews = current.interviews - suspectId,
            filedTestimonies = filed,
        )
    }

    fun fileTestimony(
        current: CaseInvestigationState,
        case: CaseData,
        testimonyId: String,
        suspectId: String,
        questionId: String,
    ): CaseInvestigationState {
        val suspect = case.suspects.firstOrNull { it.id == suspectId } ?: return current
        if (suspect.questions.none { it.id == questionId }) return current
        val asked = current.interviews[suspectId]?.askedQuestionIds.orEmpty()
        if (questionId !in asked) return current
        if (current.filedTestimonies.any { it.suspectId == suspectId && it.questionId == questionId }) return current
        return current.copy(
            filedTestimonies = current.filedTestimonies + FiledTestimony(testimonyId, suspectId, questionId),
        )
    }

    fun setSuspectNote(current: CaseInvestigationState, suspectId: String, note: String): CaseInvestigationState {
        val interview = current.interviews[suspectId] ?: InterviewState()
        return current.copy(
            suspectNotes = current.suspectNotes + (suspectId to note),
            interviews = current.interviews + (suspectId to interview.copy(notes = note, isSaved = true)),
        )
    }

    fun setItemNote(current: CaseInvestigationState, itemId: String, note: String): CaseInvestigationState =
        current.copy(itemNotes = current.itemNotes + (itemId to note))

    fun discoverItem(
        current: CaseInvestigationState,
        case: CaseData,
        itemId: String,
    ): CaseInvestigationState {
        if (case.items.none { it.id == itemId }) return current
        if (!canDiscover(current, case, itemId)) return current
        if (itemId in current.discoveredItemIds) return current

        return current.copy(
            discoveredItemIds = current.discoveredItemIds + itemId,
        )
    }

    fun setScroll(current: CaseInvestigationState, fileId: String, offset: Double): CaseInvestigationState =
        current.copy(documentScrollPositions = current.documentScrollPositions + (fileId to offset))

    fun pin(
        current: CaseInvestigationState,
        id: String,
        type: String,
        entityId: String,
        excerpt: String?,
        xPercent: Double?,
        yPercent: Double?,
    ): CaseInvestigationState {
        if (current.boardNodes.any { it.type == type && it.entityId == entityId }) return current
        val slot = current.boardNodes.size
        val col = slot % 3
        val row = (slot / 3) % 4
        val node = BoardNode(
            id = id,
            type = type,
            entityId = entityId,
            x = xPercent ?: (2.0 + col * 32.0).coerceIn(0.0, 70.0),
            y = yPercent ?: (2.0 + row * 22.0).coerceIn(0.0, 78.0),
            selectedExcerpt = excerpt,
            note = "",
        )
        return current.copy(boardNodes = current.boardNodes + node)
    }

    fun moveNode(current: CaseInvestigationState, nodeId: String, x: Double, y: Double): CaseInvestigationState =
        current.copy(
            boardNodes = current.boardNodes.map { node ->
                if (node.id == nodeId) node.copy(x = x.coerceIn(0.0, 100.0), y = y.coerceIn(0.0, 100.0)) else node
            },
        )

    fun setNodeExcerpt(current: CaseInvestigationState, nodeId: String, excerpt: String): CaseInvestigationState =
        current.copy(boardNodes = current.boardNodes.map { if (it.id == nodeId) it.copy(selectedExcerpt = excerpt) else it })

    fun setNodeNote(current: CaseInvestigationState, nodeId: String, note: String): CaseInvestigationState =
        current.copy(boardNodes = current.boardNodes.map { if (it.id == nodeId) it.copy(note = note) else it })

    fun removeNode(current: CaseInvestigationState, nodeId: String): CaseInvestigationState =
        current.copy(
            boardNodes = current.boardNodes.filter { it.id != nodeId },
            boardConnections = current.boardConnections.filter { it.fromNodeId != nodeId && it.toNodeId != nodeId },
        )

    fun connect(
        current: CaseInvestigationState,
        id: String,
        fromNodeId: String,
        toNodeId: String,
        label: String = "hilo",
    ): CaseInvestigationState {
        if (fromNodeId == toNodeId) return current
        val exists = current.boardConnections.any { connection ->
            setOf(connection.fromNodeId, connection.toNodeId) == setOf(fromNodeId, toNodeId)
        }
        if (exists) return relabel(current, fromNodeId, toNodeId, label)
        return current.copy(
            boardConnections = current.boardConnections + BoardConnection(id, fromNodeId, toNodeId, label),
        )
    }

    fun relabel(
        current: CaseInvestigationState,
        fromNodeId: String,
        toNodeId: String,
        label: String,
    ): CaseInvestigationState {
        val pair = setOf(fromNodeId, toNodeId)
        return current.copy(
            boardConnections = current.boardConnections.map { connection ->
                if (setOf(connection.fromNodeId, connection.toNodeId) == pair) connection.copy(label = label) else connection
            },
        )
    }

    fun removeConnection(current: CaseInvestigationState, connectionId: String): CaseInvestigationState =
        current.copy(boardConnections = current.boardConnections.filter { it.id != connectionId })

    fun saveDocumentFragment(
        current: CaseInvestigationState,
        case: CaseData,
        itemId: String,
        connectionId: String,
        documentId: String,
        fragment: String,
        highlights: List<HighlightRange> = emptyList(),
    ): CaseInvestigationState {
        if (case.items.none { it.id == itemId }) return current
        if (case.files.none { it.id == documentId }) return current
        val text = fragment.trim()
        if (text.isEmpty()) return current
        val memory = current.objectMemories[itemId] ?: ObjectMemory(itemId)
        if (memory.documents.size >= MEMORY_LIMIT) return current
        if (memory.documents.any { it.documentId == documentId && it.fragment == text }) return current
        val next = memory.copy(
            documents = memory.documents + DocumentConnection(
                connectionId,
                documentId,
                text,
                sanitizeHighlights(text, highlights),
            ),
        )
        return current.copy(objectMemories = current.objectMemories + (itemId to next))
    }

    fun removeDocumentFragment(
        current: CaseInvestigationState,
        itemId: String,
        connectionId: String,
    ): CaseInvestigationState {
        val memory = current.objectMemories[itemId] ?: return current
        return current.copy(
            objectMemories = current.objectMemories + (itemId to memory.copy(
                documents = memory.documents.filter { it.id != connectionId },
            )),
        )
    }

    fun saveMentalNote(
        current: CaseInvestigationState,
        case: CaseData,
        itemId: String,
        noteId: String,
        referenceType: ReferenceType,
        referenceId: String,
        text: String,
        highlights: List<HighlightRange>,
    ): CaseInvestigationState {
        if (case.items.none { it.id == itemId }) return current
        if (!referenceExists(case, referenceType, referenceId)) return current
        val leading = text.indexOfFirst { !it.isWhitespace() }.let { if (it < 0) 0 else it }
        val body = text.trim()
        if (body.isEmpty()) return current
        val memory = current.objectMemories[itemId] ?: ObjectMemory(itemId)
        val existing = memory.notes.indexOfFirst { it.id == noteId }
        if (existing < 0 && memory.notes.size >= MEMORY_LIMIT) return current
        val shifted = highlights.map { HighlightRange(it.start - leading, it.endExclusive - leading) }
        val note = MentalNote(noteId, referenceType, referenceId, body, sanitizeHighlights(body, shifted))
        val notes = if (existing < 0) memory.notes + note else memory.notes.mapIndexed { index, old ->
            if (index == existing) note else old
        }
        return current.copy(objectMemories = current.objectMemories + (itemId to memory.copy(notes = notes)))
    }

    fun removeMentalNote(
        current: CaseInvestigationState,
        itemId: String,
        noteId: String,
    ): CaseInvestigationState {
        val memory = current.objectMemories[itemId] ?: return current
        return current.copy(
            objectMemories = current.objectMemories + (itemId to memory.copy(
                notes = memory.notes.filter { it.id != noteId },
            )),
        )
    }

    private fun referenceExists(case: CaseData, type: ReferenceType, id: String): Boolean = when (type) {
        ReferenceType.PERSON -> case.suspects.any { it.id == id }
        ReferenceType.OBJECT -> case.items.any { it.id == id }
        ReferenceType.DOCUMENT -> case.files.any { it.id == id }
    }

    private fun sanitizeHighlights(text: String, ranges: List<HighlightRange>): List<HighlightRange> =
        ranges.mapNotNull { range ->
            val start = range.start
            val end = range.endExclusive
            if (start < 0 || end > text.length || end <= start) null else HighlightRange(start, end)
        }.distinctBy { it.start to it.endExclusive }.sortedBy { it.start }

    private const val MEMORY_LIMIT = 4
}
