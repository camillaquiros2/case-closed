package caseclosed.ui

import caseclosed.data.ButtonIcons
import caseclosed.data.DeskImages
import korlibs.korge.view.Container
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.position
import korlibs.korge.view.scale
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import kotlin.math.max
import kotlin.math.sin
import kotlin.time.DurationUnit

/** Labels that are not case content. Change them here, not inside the screens. */
object GameText {
    const val TITLE = "Case Closed"
    const val TAGLINE = "Todo culpable deja rastro"
    const val WINDOW_TITLE = TITLE + ": " + TAGLINE
    const val MENU_START = "Iniciar juego"
    const val MENU_CONTINUE = "Continuar"
    const val MENU_TUTORIAL = "Tutorial"
    const val MENU_EXIT = "Salir"
    const val TUTORIAL_SOON = "El tutorial llega pronto."
    const val INTRO_NEXT = "Siguiente"
    const val INTRO_SKIP = "Saltar"
    const val INTRO_TO_DESK = "Ir al escritorio"
    const val VIEW_STORY = "Ver historia"
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

    /** Shown at the bottom of the main menu. One line per entry, no button. */
    val CREDITS = listOf(
        "Zachary Hernández Valverde  ·  Camilla Quirós Torres  ·  Sebastián López Ocampo",
        "Proyecto de Ingeniería del Software 4  ·  CENFOTEC  ·  2026",
    )

    fun restartWarning(caseNumber: String): String =
        "Esto borra el tablero, las notas, las marcas, las entrevistas y las pruebas halladas de $caseNumber. El caso vuelve a abrirse. Los otros casos resueltos siguen resueltos."

    fun clueCount(count: Int): String = "$count pistas"

    fun clueSummary(clues: Int, referenced: Int): String = "$clues pistas  ·  $referenced en el tablero"
}

/**
 * Main menu. The game always opens here.
 * Title, the menu cover behind it (the case art if the cover is missing) and big buttons, no paragraphs.
 * The engine decides whether the investigation is new or resumed.
 * A new or restarted case opens its story intro first; a resumed case goes straight to the desk.
 * Exit only shows when the platform gave the navigator a way to close the app.
 */
fun buildTitle(root: Container, nav: Navigator) {
    val engine = nav.engine
    val case = engine.activeCase()
    val started = engine.isStarted()

    val cover = if (nav.assets.get(DeskImages.MENU_BACKGROUND) != null) {
        DeskImages.MENU_BACKGROUND
    } else {
        case.scenes.firstOrNull()?.image
    }
    menuBackdrop(root, nav.assets, cover)

    root.centeredText(GameText.TAGLINE.uppercase(), 214.0, 20.0, Palette.brass)
    root.centeredText(GameText.TITLE.uppercase(), 250.0, 58.0, Palette.paper)
    root.solidRect(260.0, 4.0, Palette.brass) { position((Desk.W - 260.0) / 2.0, 336.0) }
    root.centeredText(clipText("${case.caseNumber}  ·  ${case.title}", 48), 360.0, 18.0, Palette.paperShade)

    val buttonW = 460.0
    val buttonH = 84.0
    val gap = 22.0
    val x = (Desk.W - buttonW) / 2.0
    var y = 600.0

    val label = if (started) GameText.MENU_CONTINUE else GameText.MENU_START
    val icon = if (started) ButtonIcons.REANUDAR else ButtonIcons.EMPEZAR
    root.actionButton(label, x, y, buttonW, buttonH, Palette.wax, Palette.paper, nav.assets, icon) {
        if (!started) nav.banner = "${GameText.CASE_OPENED}: ${case.caseNumber}"
        engine.startCase()
        nav.go(if (started) Route.Desk else Route.CaseIntro)
    }
    y += buttonH + gap

    if (started) {
        root.actionButton(GameText.RESTART_CASE, x, y, buttonW, buttonH, Palette.brassDim, Palette.paper, nav.assets, ButtonIcons.REINICIAR) {
            root.confirmDialog(
                GameText.RESTART_TITLE,
                GameText.restartWarning(case.caseNumber),
                GameText.RESTART_CONFIRM,
                nav.assets,
                ButtonIcons.REINICIAR,
            ) {
                engine.restartCase()
                nav.banner = "${GameText.CASE_RESTARTED}: ${case.caseNumber}"
                nav.post { nav.go(Route.CaseIntro) }
            }
        }
        y += buttonH + gap
    }

    root.actionButton(GameText.MENU_TUTORIAL, x, y, buttonW, buttonH, Palette.wax, Palette.paper, nav.assets, ButtonIcons.NOTAS) {
        nav.push(Route.Tutorial)
    }
    y += buttonH + gap

    val exit = nav.onExit
    if (exit != null) {
        root.actionButton(GameText.MENU_EXIT, x, y, buttonW, buttonH, Palette.brassDim, Palette.paper, nav.assets, ButtonIcons.CERRAR) {
            exit()
        }
    }

    menuCredits(root)
}

/** The art fills the screen behind a dark veil and drifts slowly, like a title screen. Also used by the case intro. */
internal fun menuBackdrop(root: Container, assets: AssetBank, artPath: String?) {
    root.woodBackground(assets)
    val art = assets.get(artPath)
    if (art != null) {
        val cover = max(Desk.W / art.width.toDouble(), Desk.H / art.height.toDouble()) * 1.08
        val layer = root.container()
        val picture = layer.image(art) { scale(cover) }
        val baseX = (Desk.W - art.width * cover) / 2.0
        val baseY = (Desk.H - art.height * cover) / 2.0
        picture.position(baseX, baseY)
        var elapsed = 0.0
        layer.addUpdater { dt ->
            elapsed += dt.toDouble(DurationUnit.SECONDS)
            picture.position(baseX + sin(elapsed * 0.12) * 18.0, baseY + sin(elapsed * 0.09) * 12.0)
        }
    }
    root.solidRect(Desk.W, Desk.H, Palette.void) { alpha = 0.62 }
    root.solidRect(Desk.W, 220.0, Palette.void) {
        position(0.0, Desk.H - 220.0)
        alpha = 0.45
    }
}

/** Credits sit at the bottom of the menu. They are not a button. */
private fun menuCredits(root: Container) {
    val lineH = 26.0
    var y = Desk.H - 40.0 - GameText.CREDITS.size * lineH
    root.solidRect(120.0, 2.0, Palette.brassDim) { position((Desk.W - 120.0) / 2.0, y - 18.0) }
    for (line in GameText.CREDITS) {
        root.centeredText(line, y, 15.0, Palette.paperShade).alpha = 0.75
        y += lineH
    }
}

private fun Container.centeredText(line: String, y: Double, size: Double, color: RGBAFill) =
    text(line, textSize = size, color = color).also { view ->
        val measured = view.width.toDouble()
        val used = if (measured > 1.0) measured else line.length * size * 0.52
        view.position((Desk.W - used) / 2.0, y)
        view.mouseEnabled = false
    }
