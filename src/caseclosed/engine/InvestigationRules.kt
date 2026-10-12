package caseclosed.engine

import caseclosed.data.CaseCatalog
import caseclosed.model.BoardConnection
import caseclosed.model.BoardNode
import caseclosed.model.CaseData
import caseclosed.model.CaseInvestigationState
import caseclosed.model.HighlightEntry
import caseclosed.model.InterviewState
import caseclosed.model.InterviewTurn
import caseclosed.model.Question

/** Pure edits to one case investigation. The engine supplies ids and timestamps. */
internal object InvestigationRules {
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

    fun resetInterview(current: CaseInvestigationState, suspectId: String): CaseInvestigationState =
        if (suspectId !in current.interviews) current
        else current.copy(interviews = current.interviews - suspectId)

    fun setSuspectNote(current: CaseInvestigationState, suspectId: String, note: String): CaseInvestigationState {
        val interview = current.interviews[suspectId] ?: InterviewState()
        return current.copy(
            suspectNotes = current.suspectNotes + (suspectId to note),
            interviews = current.interviews + (suspectId to interview.copy(notes = note, isSaved = true)),
        )
    }

    fun setItemNote(current: CaseInvestigationState, itemId: String, note: String): CaseInvestigationState =
        current.copy(itemNotes = current.itemNotes + (itemId to note))

    fun discoverItem(current: CaseInvestigationState, itemId: String): CaseInvestigationState =
        if (itemId in current.discoveredItemIds) current
        else current.copy(discoveredItemIds = current.discoveredItemIds + itemId)

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
    ): CaseInvestigationState {
        if (fromNodeId == toNodeId) return current
        val exists = current.boardConnections.any { connection ->
            setOf(connection.fromNodeId, connection.toNodeId) == setOf(fromNodeId, toNodeId)
        }
        if (exists) return current
        return current.copy(
            boardConnections = current.boardConnections + BoardConnection(id, fromNodeId, toNodeId),
        )
    }

    fun removeConnection(current: CaseInvestigationState, connectionId: String): CaseInvestigationState =
        current.copy(boardConnections = current.boardConnections.filter { it.id != connectionId })
}
