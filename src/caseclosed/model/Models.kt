package caseclosed.model

import kotlinx.serialization.Serializable

/**
 * Static case content. The engine never branches on a case id;
 * new investigations are added by registering another [CasePack].
 */

data class CasePack(
    val data: CaseData,
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
    /** Overhead locations the detective can walk. Empty for a desk-only case. */
    val scenes: List<CrimeScene> = emptyList(),
    val solution: CaseSolution,
    val phoneMessages: List<PhoneMessage>,
    val discoveries: List<DiscoveryDef> = emptyList(),
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
    val requiredDiscoveryIds: List<String> = emptyList(),
)

enum class ItemOrigin {
    /** Already in the locker when the case opens, because the files brought it in. */
    FILE,
    /** Sitting in a scene until the detective sends it to the desk. */
    SCENE,
}

data class Item(
    val id: String,
    val name: String,
    val image: String,
    val shortNotes: String,
    val description: String,
    val findings: String,
    val relatedFileIds: List<String> = emptyList(),
    /** False for objects that can be picked up but do not bear on the charge. */
    val relevant: Boolean = true,
    val origin: ItemOrigin = ItemOrigin.SCENE,
)

/** A rectangle in percentages of its image. Origin is the top left. 0–100. */
data class PercentBox(
    val x: Double,
    val y: Double,
    val w: Double,
    val h: Double,
)

data class SceneItemSpot(
    val itemId: String,
    val bounds: PercentBox,
)

data class SubScene(
    val id: String,
    val name: String,
    /** Close view. `images/cases/<case>/scenes/<scene>/<id>.png` */
    val image: String,
    /** Where this part sits on the overhead image. */
    val bounds: PercentBox,
    val items: List<SceneItemSpot> = emptyList(),
)

data class CrimeScene(
    val id: String,
    val name: String,
    /** Photograph from above. `images/cases/<case>/scenes/<id>/overhead.png` */
    val image: String,
    val subscenes: List<SubScene>,
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

data class DiscoveryDef(
    val id: String,
    val requiredDiscoveryIds: List<String> = emptyList(),
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
