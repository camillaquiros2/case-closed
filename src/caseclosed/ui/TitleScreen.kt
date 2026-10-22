package caseclosed.ui

import caseclosed.model.CaseData
import caseclosed.model.CaseInvestigationState
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.position
import korlibs.korge.view.solidRect
import korlibs.korge.view.text

/** Labels that are not case content. Change them here, not inside the screens. */
object GameText {
    const val TITLE = "Detective de mesa"
    const val TAGLINE = "Casos fríos"
    const val WINDOW_TITLE = TITLE + ": " + TAGLINE
    const val ON_THE_DESK = "EN EL ESCRITORIO"
    const val NEW_CASE = "Investigación nueva"
    const val START_CASE = "Empezar caso"
    const val RESUME_CASE = "Reanudar caso"
    const val RESTART_CASE = "Reiniciar caso"
    const val RESTART_TITLE = "¿Reiniciar este caso?"
    const val RESTART_CONFIRM = "Reiniciar"
    const val MAIN_MENU = "Menú principal"
    const val CASE_OPENED = "Investigación abierta"
    const val CASE_RESTARTED = "Investigación reiniciada"
    const val CLUE_LOG = "Bitácora"
    const val CLUE_LOG_EMPTY = "Aún no hay pistas. Marca pasajes en los expedientes, interroga a los sospechosos y registra la escena."
    const val CLUE_EVIDENCE = "Pruebas"
    const val CLUE_EXCERPTS = "Extractos"
    const val CLUE_TESTIMONY = "Testimonio"
    const val VIEW_SOURCE = "Ver origen"
    const val ADD_TO_BOARD = "Al tablero"
    const val OPEN_BOARD = "Abrir tablero"
    const val ON_THE_BOARD = "EN EL TABLERO"
    const val FROM = "De"
    const val NOTE = "Nota"

    fun inProgress(logged: Int, pins: Int): String = "En curso  ·  $logged registradas  ·  $pins fichas"

    fun restartWarning(caseNumber: String): String =
        "Esto borra el tablero, las notas, las marcas, las entrevistas y las pruebas halladas de $caseNumber. Los casos resueltos siguen resueltos."

    fun clueCount(count: Int): String = "$count pistas"

    fun clueSummary(clues: Int, referenced: Int): String = "$clues pistas  ·  $referenced en el tablero"
}

/**
 * Main screen. Shows the case that is on the desk and opens it.
 * The engine decides whether the investigation is new or resumed.
 * A started case can also be restarted from its authored starting state.
 */
fun buildTitle(root: Container, nav: Navigator) {
    val engine = nav.engine
    val case = engine.activeCase()
    val started = engine.isStarted()
    val left = 32.0
    val width = Desk.W - left * 2
    val headerH = 220.0

    root.woodBackground(nav.assets)
    root.solidRect(Desk.W, headerH, Palette.mahoganyDeep) { alpha = 0.92 }
    root.solidRect(Desk.W, 5.0, Palette.brass) { position(0.0, headerH - 5.0) }
    root.text(GameText.TITLE, textSize = 48.0, color = Palette.paper) { position(left, 70.0) }
    root.text(GameText.TAGLINE, textSize = 22.0, color = Palette.brass) { position(left, 134.0) }

    val cardY = headerH + 40.0
    val cardH = caseCard(root, case, engine.investigation(), started, left, cardY, width)
    val buttonY = cardY + cardH + 32.0

    val label = if (started) GameText.RESUME_CASE else GameText.START_CASE
    root.actionButton(label, left, buttonY, width, 72.0, Palette.wax, Palette.paper) {
        if (!started) nav.banner = "${GameText.CASE_OPENED}: ${case.caseNumber}"
        engine.startCase()
        nav.go(Route.Desk)
    }

    if (started) {
        root.actionButton(GameText.RESTART_CASE, left, buttonY + 88.0, width, 60.0, Palette.brassDim, Palette.paper) {
            root.confirmDialog(
                GameText.RESTART_TITLE,
                GameText.restartWarning(case.caseNumber),
                GameText.RESTART_CONFIRM,
            ) {
                engine.restartCase()
                nav.banner = "${GameText.CASE_RESTARTED}: ${case.caseNumber}"
                nav.go(Route.Desk)
            }
        }
    }
}

/** Draws the case summary and returns its height so the button can sit below it. */
private fun caseCard(
    root: Container,
    case: CaseData,
    state: CaseInvestigationState,
    started: Boolean,
    x: Double,
    y: Double,
    w: Double,
): Double {
    val pad = 28.0
    val inner = w - pad * 2
    val card = root.container { position(x, y) }
    val frame = card.container()

    var cy = pad
    card.text(GameText.ON_THE_DESK, textSize = 14.0, color = Palette.inkSoft) { position(pad, cy) }
    cy += 22.0
    card.text(case.caseNumber, textSize = 16.0, color = Palette.wax) { position(pad, cy) }
    cy += 34.0
    cy += card.blockText(case.title, pad, cy, inner, 28.0, Palette.ink) + 12.0
    card.text(clipText("${case.location}  ·  ${case.date}", 70), textSize = 16.0, color = Palette.inkSoft) {
        position(pad, cy)
    }
    cy += 26.0
    card.text(clipText("${case.victim}  ·  ${case.subtitle}", 70), textSize = 16.0, color = Palette.inkSoft) {
        position(pad, cy)
    }
    cy += 36.0
    card.solidRect(inner, 2.0, Palette.brassDim) { position(pad, cy) }
    cy += 18.0
    cy += card.blockText(case.briefing, pad, cy, inner, 19.0, Palette.ink) + 20.0

    val status = if (started) {
        GameText.inProgress(state.discoveredItemIds.size, state.boardNodes.size)
    } else {
        GameText.NEW_CASE
    }
    card.text(status, textSize = 16.0, color = Palette.wax) { position(pad, cy) }
    cy += 22.0 + pad

    frame.framedRect(w, cy, Palette.paper)
    return cy
}
