package caseclosed.ui

import caseclosed.model.ClueEntry
import caseclosed.model.ClueKind
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.position
import korlibs.korge.view.solidRect
import korlibs.korge.view.text

private const val LOG_CARD_W = 650.0
private const val LOG_PAD = 16.0
private const val TEXT_PREVIEW = 220

/** Screen-only memory so the log reopens where the detective left it after viewing a source. */
private object ClueLogView {
    var scroll = 0.0
    var selectedId: String? = null
}

/**
 * Every clue the detective has found, grouped by kind.
 * Tap a card to select it, then open its source or send it to the evidence board.
 */
fun buildClueLog(root: Container, nav: Navigator) {
    val engine = nav.engine
    val case = engine.activeCase()
    val entries = engine.clueLog()
    val referenced = entries.count { engine.isReferenced(it) }
    root.solidRect(Desk.W, Desk.H, Palette.mahoganyDeep)
    root.chrome(GameText.CLUE_LOG, case.title, nav, true)
    root.scrollBox(
        x = 24.0,
        y = Desk.HEADER + 12.0,
        w = 672.0,
        h = Desk.H - Desk.HEADER - 24.0,
        initial = ClueLogView.scroll,
        onScroll = { ClueLogView.scroll = it },
    ) {
        var y = 8.0
        text(GameText.clueSummary(entries.size, referenced), textSize = 15.0, color = Palette.brass) {
            position(8.0, y)
        }
        y += 36.0
        if (entries.isEmpty()) {
            y += blockText(GameText.CLUE_LOG_EMPTY, 8.0, y, 640.0, 18.0, Palette.paper)
        }
        for (kind in ClueKind.entries) {
            val group = entries.filter { it.kind == kind }
            if (group.isEmpty()) continue
            text(kindLabel(kind), textSize = 20.0, color = Palette.paper) { position(8.0, y) }
            y += 34.0
            for (entry in group) {
                y += clueCard(this, nav, entry, y)
            }
        }
        y + 12.0
    }
}

/** Draws one clue and returns its height. Fill shows "on the board", frame shows "selected". */
private fun clueCard(parent: Container, nav: Navigator, entry: ClueEntry, y: Double): Double {
    val engine = nav.engine
    val selected = entry.id == ClueLogView.selectedId
    val pinned = engine.isReferenced(entry)
    val inner = LOG_CARD_W - LOG_PAD * 2
    val card = parent.container { position(0.0, y) }
    val frame = card.container()

    var cy = LOG_PAD
    if (pinned) {
        card.text(GameText.ON_THE_BOARD, textSize = 13.0, color = Palette.wax) { position(LOG_PAD, cy) }
        cy += 22.0
    }
    cy += card.blockText(entry.title, LOG_PAD, cy, inner, 20.0, Palette.ink) + 6.0
    cy += card.blockText(clipText(entry.text, TEXT_PREVIEW), LOG_PAD, cy, inner, 16.0, Palette.ink) + 6.0
    if (entry.origin.isNotBlank()) {
        cy += card.blockText("${GameText.FROM}: ${entry.origin}", LOG_PAD, cy, inner, 14.0, Palette.inkSoft) + 4.0
    }
    if (entry.note.isNotBlank()) {
        cy += card.blockText("${GameText.NOTE}: ${entry.note}", LOG_PAD, cy, inner, 14.0, Palette.inkSoft) + 4.0
    }
    cy += 8.0

    // The tap area sits under the buttons so they keep their own clicks.
    val hit = card.container()
    if (selected) {
        card.actionButton(GameText.VIEW_SOURCE, LOG_PAD, cy, 290.0, 48.0, Palette.greenFelt, Palette.paper) {
            nav.push(sourceRoute(entry))
        }
        if (pinned) {
            card.actionButton(GameText.OPEN_BOARD, LOG_PAD + 306.0, cy, 290.0, 48.0, Palette.brassDim, Palette.paper) {
                nav.push(Route.Board)
            }
        } else {
            card.actionButton(GameText.ADD_TO_BOARD, LOG_PAD + 306.0, cy, 290.0, 48.0, Palette.wax, Palette.paper) {
                engine.addReference(entry)
                nav.refresh()
            }
        }
        cy += 48.0 + LOG_PAD
    }

    val h = cy
    frame.framedRect(
        LOG_CARD_W,
        h,
        fill = if (pinned) Palette.highlight else Palette.paper,
        frame = if (selected) Palette.wax else Palette.brass,
    )
    hit.hitRect(LOG_CARD_W, h) {
        ClueLogView.selectedId = if (selected) null else entry.id
        nav.refresh()
    }
    return h + 16.0
}

private fun sourceRoute(entry: ClueEntry): Route = when (entry.kind) {
    ClueKind.EVIDENCE -> Route.ItemDetail(entry.sourceId)
    ClueKind.DOCUMENT_EXCERPT -> Route.Document(entry.sourceId)
    ClueKind.TESTIMONY -> Route.Interview(entry.sourceId)
}

private fun kindLabel(kind: ClueKind): String = when (kind) {
    ClueKind.EVIDENCE -> GameText.CLUE_EVIDENCE
    ClueKind.DOCUMENT_EXCERPT -> GameText.CLUE_EXCERPTS
    ClueKind.TESTIMONY -> GameText.CLUE_TESTIMONY
}
