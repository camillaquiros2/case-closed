package caseclosed.ui

import caseclosed.data.ButtonIcons
import caseclosed.data.DeskImages
import korlibs.image.bitmap.Bitmap
import korlibs.image.color.Colors
import korlibs.korge.input.onClick
import korlibs.korge.input.onOut
import korlibs.korge.input.onOver
import korlibs.korge.view.Container
import korlibs.korge.view.View
import korlibs.korge.view.addUpdater
import korlibs.korge.view.anchor
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.position
import korlibs.korge.view.rotation
import korlibs.korge.view.scale
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import korlibs.math.geom.degrees
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.DurationUnit

fun buildDesk(root: Container, nav: Navigator) {
    val engine = nav.engine
    val case = engine.activeCase()
    val state = engine.investigation()
    root.woodBackground(nav.assets)
    val weather = DeskWeather()
    weather.behind(root)
    root.solidRect(Desk.W, 132.0, Palette.mahoganyDeep) { alpha = 0.92 }
    root.solidRect(Desk.W, 5.0, Palette.brass) { position(0.0, 127.0) }
    root.text(case.caseNumber, textSize = 14.0, color = Palette.brass) { position(28.0, 16.0) }
    root.text(clipText(case.title, 32), textSize = 28.0, color = Palette.paper) { position(28.0, 40.0) }
    root.text("${case.location}  ·  ${case.date}", textSize = 15.0, color = Palette.paperShade) { position(28.0, 82.0) }
    if (engine.isCompleted(case.id)) {
        root.text("CERRADO", textSize = 16.0, color = Palette.gold) { position(28.0, 104.0) }
    }
    root.actionButton("Casos", Desk.W - 168.0, 32.0, 140.0, 56.0, Palette.brass, Palette.ink, nav.assets, ButtonIcons.CASOS) {
        openCaseDrawer(root, nav)
    }

    val banner = nav.banner
    if (!banner.isNullOrBlank()) {
        root.solidRect(656.0, 44.0, Palette.gold) { position(32.0, 142.0) }
        root.text(clipText(banner, 70), textSize = 16.0, color = Palette.ink) { position(44.0, 154.0) }
        nav.banner = null
    }

    val top = if (banner.isNullOrBlank()) 148.0 else 196.0
    val phoneX = 18.0
    val phoneY = top
    val phoneSize = 196.0
    deskSprite(root, phoneX, phoneY, phoneSize, Palette.phone, "Teléfono", phoneLabel(engine), nav.assets, DeskImages.PHONE, tilt = -6.0) {
        nav.push(Route.Phone)
    }
    if (engine.unreadCount() > 0) {
        root.solidRect(36.0, 36.0, Palette.wax) { position(phoneX + phoneSize - 28.0, phoneY - 6.0) }
        root.text(engine.unreadCount().toString(), textSize = 18.0, color = Palette.paper) {
            position(phoneX + phoneSize - 16.0, phoneY + 2.0)
        }
    }
    deskSprite(
        root, 214.0, top + 18.0, 228.0, Palette.manila, "Expedientes",
        "${case.files.size} documentos", nav.assets, DeskImages.FOLDER, tilt = 3.0,
    ) { nav.push(Route.Files) }
    deskSprite(
        root, 468.0, top - 6.0, 220.0, Palette.dossier, "Sospechosos",
        "${case.suspects.size} personas", nav.assets, DeskImages.SUSPECTS, tilt = -2.0,
    ) { nav.push(Route.Suspects) }

    val lower = top + 280.0
    deskSprite(
        root, 12.0, lower, 198.0, Palette.locker, "Casillero",
        "${state.discoveredItemIds.size} fichas", nav.assets, DeskImages.LOCKER, tilt = 2.0,
    ) { nav.push(Route.Items) }
    deskSprite(
        root, 206.0, lower + 22.0, 216.0, Palette.greenFelt, GameText.CLUE_LOG,
        GameText.clueCount(engine.clueLog().size), nav.assets, DeskImages.NOTEBOOK, tilt = -4.0,
    ) { nav.push(Route.Clues) }
    deskSprite(
        root, 448.0, lower - 8.0, 242.0, Palette.cork, "Tablero",
        "${state.boardNodes.size} fichas  ·  ${state.boardConnections.size} hilos",
        nav.assets, DeskImages.BOARD, tilt = 3.0,
    ) { nav.push(Route.Board) }

    val docketY = lower + 286.0
    val docketW = 400.0
    val docketH = docketW * 724.0 / 2172.0
    deskBanner(root, 16.0, docketY, docketW, docketH, nav.assets, DeskImages.ACCUSATION) {
        nav.push(Route.Conclusion)
    }

    root.blockText(case.briefing, 36.0, docketY + docketH + 16.0, 648.0, 17.0, Palette.paper)
    root.text("${case.victim}  ·  ${case.subtitle}", textSize = 16.0, color = Palette.brass) {
        position(36.0, Desk.H - 132.0)
    }
    if (case.scenes.isNotEmpty()) {
        root.actionButton(
            "Visitar la escena",
            32.0,
            Desk.H - 96.0,
            656.0,
            68.0,
            Palette.greenFelt,
            Palette.paper,
            nav.assets,
            ButtonIcons.ESCENA,
        ) {
            val scenes = case.scenes
            if (scenes.size == 1) nav.push(Route.SceneMap(scenes.first().id))
            else nav.push(Route.SceneList)
        }
    }
    weather.front(root)
}

private fun phoneLabel(engine: caseclosed.engine.GameEngine): String {
    val unread = engine.unreadCount()
    return if (unread == 0) "Sin llamadas" else "$unread sin leer"
}

private fun deskSprite(
    root: Container,
    x: Double,
    y: Double,
    size: Double,
    fill: RGBAFill,
    title: String,
    subtitle: String,
    assets: AssetBank,
    imagePath: String,
    tilt: Double,
    onPress: () -> Unit,
) {
    val slot = root.container { position(x, y) }
    slot.contactShadow(size, size * 0.84)
    val bmp = assets.get(imagePath)
    if (bmp != null) {
        val picture = slot.fitSprite(bmp, size, tilt)
        slot.caption(title, size + 4.0, size, 16.0, Palette.paper)
        slot.caption(subtitle, size + 22.0, size, 13.0, Palette.brass)
        slot.hitHover(size, size + 42.0, picture, onPress)
    } else {
        slot.solidRect(size, size, Palette.brass)
        slot.solidRect(size - 12.0, size - 12.0, fill) { position(6.0, 6.0) }
        slot.text(title, textSize = 20.0, color = Palette.paper) { position(14.0, size - 64.0) }
        slot.text(subtitle, textSize = 14.0, color = Palette.paperShade) { position(14.0, size - 36.0) }
        slot.hitRect(size, size) { onPress() }
    }
}

private fun deskBanner(
    root: Container,
    x: Double,
    y: Double,
    w: Double,
    h: Double,
    assets: AssetBank,
    imagePath: String,
    onPress: () -> Unit,
) {
    val slot = root.container { position(x, y) }
    slot.contactShadow(w, h - 6.0)
    val bmp = assets.get(imagePath)
    if (bmp != null) {
        val picture = slot.image(bmp) {
            anchor(0.5, 0.5)
            position(w / 2.0, h / 2.0)
            scale(w / bmp.width.toDouble(), h / bmp.height.toDouble())
        }
        slot.hitHover(w, h, picture, onPress)
    } else {
        slot.solidRect(w, h, Palette.brass)
        slot.solidRect(w - 12.0, h - 12.0, Palette.wax) { position(6.0, 6.0) }
        slot.text("Acusación", textSize = 22.0, color = Palette.paper) { position(24.0, 28.0) }
        slot.text("Nombra a un sospechoso y presenta el cargo", textSize = 15.0, color = Palette.paperShade) {
            position(24.0, 64.0)
        }
        slot.hitRect(w, h) { onPress() }
    }
}

private fun Container.fitSprite(bmp: Bitmap, size: Double, tilt: Double): View {
    val bw = bmp.width.toDouble().coerceAtLeast(1.0)
    val bh = bmp.height.toDouble().coerceAtLeast(1.0)
    val fitted = min(size / bw, size / bh)
    return image(bmp) {
        anchor(0.5, 0.5)
        position(size / 2.0, size / 2.0)
        scale(fitted)
        rotation = tilt.degrees
    }
}

private fun Container.hitHover(w: Double, h: Double, picture: View, onPress: () -> Unit) {
    val restScaleX = picture.scaleX
    val restScaleY = picture.scaleY
    val restY = picture.y
    var hovering = false
    var shown = 0.0
    solidRect(w, h, Colors.WHITE) {
        alpha = 0.01
        onClick { onPress() }
        onOver { hovering = true }
        onOut { hovering = false }
    }
    addUpdater { dt ->
        val step = dt.toDouble(DurationUnit.SECONDS).coerceAtMost(0.05)
        val target = if (hovering) 1.0 else 0.0
        shown += (target - shown) * (step * 14.0).coerceAtMost(1.0)
        val grow = 1.0 + shown * 0.07
        picture.scaleX = restScaleX * grow
        picture.scaleY = restScaleY * grow
        picture.y = restY - shown * 12.0
    }
}

private fun Container.contactShadow(width: Double, y: Double) {
    val w = width * 0.58
    val x = (width - w) / 2.0 + 8.0
    solidRect(w, 14.0, Colors["#120C09"]) {
        position(x, y)
        alpha = 0.34
        mouseEnabled = false
    }
    solidRect(w * 0.62, 7.0, Colors["#120C09"]) {
        position(x + w * 0.19, y + 3.0)
        alpha = 0.22
        mouseEnabled = false
    }
}

/** Soft light over the desk, with an occasional flash. No window frame and no rain. */
private class DeskWeather {
    private var glass: View? = null
    private var elapsed = 0.0
    private var nextFlash = 3.4
    private var flashLeft = 0.0

    fun behind(root: Container) {
        glass = root.solidRect(Desk.W, 250.0, Colors["#D7E6F4"]) {
            position(0.0, 132.0)
            alpha = 0.07
            mouseEnabled = false
        }
    }

    fun front(root: Container) {
        val flash = root.solidRect(Desk.W, Desk.H, Colors["#F4F8FF"]) {
            alpha = 0.0
            mouseEnabled = false
        }
        root.addUpdater { dt ->
            val step = dt.toDouble(DurationUnit.SECONDS)
            elapsed += step
            val glow = glass
            if (glow != null) {
                glow.alpha = 0.06 + 0.025 * sin(elapsed * 0.65)
            }
            if (flashLeft > 0.0) {
                flashLeft -= step
                val bolt = when {
                    flashLeft > 0.22 -> 0.20
                    flashLeft > 0.14 -> 0.03
                    flashLeft > 0.0 -> 0.16 * (flashLeft / 0.14)
                    else -> 0.0
                }
                flash.alpha = bolt
                if (glow != null) glow.alpha = 0.08 + bolt
                if (flashLeft <= 0.0) flash.alpha = 0.0
            } else if (elapsed >= nextFlash) {
                flashLeft = 0.32
                nextFlash = elapsed + 5.5 + Random.nextDouble() * 5.0
            }
        }
    }
}

private fun Container.caption(line: String, y: Double, boxW: Double, size: Double, color: RGBAFill) {
    val view = text(line, textSize = size, color = color)
    val measured = view.width.toDouble()
    val used = if (measured > 1.0) measured else line.length * size * 0.52
    view.position((boxW - used) / 2.0, y)
}

private fun openCaseDrawer(root: Container, nav: Navigator) {
    root.overlay(660.0, 720.0, dismissOnScrim = true) { dismiss ->
        text("Archivo de casos", textSize = 28.0, color = Palette.ink) { position(0.0, 0.0) }
        text("Expedientes abiertos", textSize = 16.0, color = Palette.inkSoft) { position(0.0, 40.0) }
        var y = 80.0
        val unlocked = nav.engine.progression.unlockedCaseIds
        if (unlocked.size < 2) {
            blockText(
                "El siguiente expediente sigue en el armario del jefe.",
                0.0, y, 580.0, 18.0, Palette.ink,
            )
            y += 80.0
        }
        for (id in unlocked) {
            val pack = caseclosed.data.CaseCatalog.require(id)
            val data = pack.data
            val active = id == nav.engine.progression.activeCaseId
            val closed = nav.engine.isCompleted(id)
            val label = buildString {
                append(data.caseNumber)
                append("  ")
                append(data.title)
                if (active) append("   ·  EN EL ESCRITORIO")
                if (closed) append("   ·  CERRADO")
            }
            solidRect(590.0, 88.0, if (active) Palette.highlight else Palette.paperShade) { position(0.0, y) }
            text(clipText(label, 52), textSize = 16.0, color = Palette.ink) { position(14.0, y + 16.0) }
            text(data.subtitle, textSize = 14.0, color = Palette.inkSoft) { position(14.0, y + 46.0) }
            val rowY = y
            solidRect(590.0, 88.0, korlibs.image.color.Colors.WHITE) {
                position(0.0, rowY)
                alpha = 0.01
                onClick {
                    nav.engine.setActiveCase(id)
                    dismiss()
                    if (nav.engine.isStarted()) nav.refresh() else nav.go(Route.Title)
                }
            }
            y += 100.0
        }
        actionButton("Cerrar", 0.0, 560.0, 200.0, 56.0, Palette.brassDim, Palette.paper, nav.assets, ButtonIcons.CERRAR) { dismiss() }
        actionButton(GameText.MAIN_MENU, 216.0, 560.0, 240.0, 56.0, Palette.wax, Palette.paper, nav.assets, ButtonIcons.MENU) {
            dismiss()
            nav.go(Route.Title)
        }
    }
}
