package caseclosed.model

import kotlinx.serialization.Serializable

/**
 * Static case content. The engine never branches on a case id;
 * new investigations are added by registering another [CasePack].
 */

data class CasePack(
    val data: CaseData,
    /** Items already logged when a fresh investigation is opened. */
    val initialDiscoveredItemIds: List<String> = emptyList(),
)

data class CaseData(
    val id: String,
    val caseNumber: String,
    val title: String,
    val subtitle: String,
    val briefing: String,
    val victim: String,
    val location: String,
    val date: String,
    val files: List<CaseFile>,
    val suspects: List<Suspect>,
    val items: List<Item>,
    val solution: CaseSolution,
    val phoneMessages: List<PhoneMessage>,
)

data class CaseFile(
    val id: String,
    val title: String,
    val documentNumber: String,
    val date: String,
    val thumbnail: String,
    val shortDescription: String,
    val sections: List<FileSection>,
    val relatedSuspectIds: List<String> = emptyList(),
    val relatedItemIds: List<String> = emptyList(),
)

data class FileSection(
    val id: String,
    val heading: String,
    val content: String,
    val image: ImageRef? = null,
    val highlightableClues: List<ClueDef>? = null,
    val references: List<DocReference>? = null,
)

data class ImageRef(val src: String, val caption: String)

data class ClueDef(val id: String, val text: String, val description: String)

data class DocReference(
    val type: String,
    val targetId: String,
    val label: String,
)

data class Suspect(
    val id: String,
    val name: String,
    val alias: String,
    val portrait: String,
    val shortDescription: String,
    val occupation: String,
    val knownInformation: String,
    val alibi: String,
    val questions: List<Question>,
)

data class Question(
    val id: String,
    val question: String,
    val answer: String,
    /** Item id logged into the evidence locker when this answer is heard. */
    val revealsItem: String? = null,
    /** Stable highlight id created from the matching document clue, if any. */
    val revealsClue: String? = null,
)

data class Item(
    val id: String,
    val name: String,
    val image: String,
    val shortNotes: String,
    val description: String,
    val findings: String,
    val relatedFileIds: List<String> = emptyList(),
)

data class CaseSolution(
    val guiltySuspectId: String,
    val requiredEvidenceIds: List<String>,
    val minEvidenceCount: Int,
    val explanation: String,
    val failureHints: Map<String, String>,
)

data class PhoneMessage(
    val id: String,
    val caller: String,
    val time: String,
    val message: String,
    val unread: Boolean,
)

@Serializable
data class GlobalProgression(
    val activeCaseId: String = "case_001",
    val unlockedCaseIds: List<String> = listOf("case_001"),
    val completedCaseIds: List<String> = emptyList(),
    /** Cases opened from the title screen. Their investigation is kept, not rebuilt. */
    val startedCaseIds: List<String> = emptyList(),
)

@Serializable
data class BoardNode(
    val id: String,
    val type: String,
    val entityId: String,
    val x: Double,
    val y: Double,
    val selectedExcerpt: String? = null,
    val note: String? = null,
)

@Serializable
data class BoardConnection(
    val id: String,
    val fromNodeId: String,
    val toNodeId: String,
)

@Serializable
data class HighlightEntry(
    val id: String,
    val sourceType: String,
    val sourceId: String,
    val sectionId: String? = null,
    val questionId: String? = null,
    val text: String,
    val note: String,
    val createdAt: Long,
)

@Serializable
data class InterviewTurn(
    val questionId: String,
    val timestamp: Long,
)

@Serializable
data class InterviewState(
    val askedQuestionIds: List<String> = emptyList(),
    val conversationOrder: List<InterviewTurn> = emptyList(),
    val notes: String? = null,
    val isSaved: Boolean? = null,
)

@Serializable
data class CaseInvestigationState(
    val boardNodes: List<BoardNode> = emptyList(),
    val boardConnections: List<BoardConnection> = emptyList(),
    val highlights: List<HighlightEntry> = emptyList(),
    val interviews: Map<String, InterviewState> = emptyMap(),
    val suspectNotes: Map<String, String> = emptyMap(),
    val itemNotes: Map<String, String> = emptyMap(),
    val discoveredItemIds: List<String> = emptyList(),
    val documentScrollPositions: Map<String, Double> = emptyMap(),
    /** Message ids the detective has already opened. Authored `unread` flags live on the case data. */
    val readMessageIds: List<String> = emptyList(),
)

sealed class Verdict {
    data class Solved(val explanation: String) : Verdict()
    data class Rejected(val critique: String) : Verdict()
}
