package caseclosed.ui

import korlibs.korge.input.onClick
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.position
import korlibs.korge.view.scale
import korlibs.korge.view.solidRect
import korlibs.korge.view.text

fun buildDesk(root: Container, nav: Navigator) {
    val engine = nav.engine
    val case = engine.activeCase()
    val state = engine.investigation()
    root.woodBackground(nav.assets)
    root.solidRect(Desk.W, 132.0, Palette.mahoganyDeep) { alpha = 0.92 }
    root.solidRect(Desk.W, 5.0, Palette.brass) { position(0.0, 127.0) }
    root.text(case.caseNumber, textSize = 14.0, color = Palette.brass) { position(28.0, 16.0) }
    root.text(clipText(case.title, 32), textSize = 28.0, color = Palette.paper) { position(28.0, 40.0) }
    root.text("${case.location}  ·  ${case.date}", textSize = 15.0, color = Palette.paperShade) { position(28.0, 82.0) }
    if (engine.isCompleted(case.id)) {
        root.text("CLOSED", textSize = 16.0, color = Palette.gold) { position(28.0, 104.0) }
    }
    root.actionButton("Cases", Desk.W - 160.0, 36.0, 128.0, 52.0, Palette.brass, Palette.ink) {
        openCaseDrawer(root, nav)
    }

    val banner = nav.banner
    if (!banner.isNullOrBlank()) {
        root.solidRect(656.0, 44.0, Palette.gold) { position(32.0, 142.0) }
        root.text(clipText(banner, 70), textSize = 16.0, color = Palette.ink) { position(44.0, 154.0) }
        nav.banner = null
    }

    val top = if (banner.isNullOrBlank()) 156.0 else 204.0
    val cardW = 208.0
    val gap = 16.0
    val left = 32.0
    deskCard(root, left, top, cardW, 188.0, Palette.phone, "Telephone", phoneLabel(engine), nav.assets, null) {
        nav.push(Route.Phone)
    }
    if (engine.unreadCount() > 0) {
        root.solidRect(36.0, 36.0, Palette.wax) { position(left + cardW - 28.0, top - 8.0) }
        root.text(engine.unreadCount().toString(), textSize = 18.0, color = Palette.paper) {
            position(left + cardW - 16.0, top)
        }
    }
    deskCard(
        root, left + cardW + gap, top, cardW, 188.0, Palette.manila, "Case Files",
        "${case.files.size} documents", nav.assets, null,
    ) { nav.push(Route.Files) }
    deskCard(
        root, left + (cardW + gap) * 2, top, cardW, 188.0, Palette.dossier, "Suspects",
        "${case.suspects.size} persons", nav.assets, null,
    ) { nav.push(Route.Suspects) }

    val mid = top + 208.0
    val wide = 320.0
    deskCard(
        root, left, mid, wide, 220.0, Palette.locker, "Evidence Locker",
        "${state.discoveredItemIds.size} logged", nav.assets, null,
    ) { nav.push(Route.Items) }
    deskCard(
        root, left + wide + gap, mid, wide, 220.0, Palette.cork, "Evidence Board",
        "${state.boardNodes.size} pins  ·  ${state.boardConnections.size} strings",
        nav.assets, "images/desk/cork_texture.png",
    ) { nav.push(Route.Board) }

    val docketY = mid + 244.0
    deskCard(root, left, docketY, 656.0, 150.0, Palette.wax, "Indictment Docket", "Name a suspect and file the charge", nav.assets, null) {
        nav.push(Route.Conclusion)
    }

    root.blockText(case.briefing, 36.0, docketY + 168.0, 648.0, 17.0, Palette.paper)
    root.text("${case.victim}  ·  ${case.subtitle}", textSize = 16.0, color = Palette.brass) {
        position(36.0, Desk.H - 132.0)
    }
    if (case.scenes.isNotEmpty()) {
        root.actionButton(
            "Visit the Scene",
            32.0,
            Desk.H - 96.0,
            656.0,
            68.0,
            Palette.greenFelt,
            Palette.paper,
        ) {
            val scenes = case.scenes
            if (scenes.size == 1) nav.push(Route.SceneMap(scenes.first().id))
            else nav.push(Route.SceneList)
        }
    }
}

private fun phoneLabel(engine: caseclosed.engine.GameEngine): String {
    val unread = engine.unreadCount()
    return if (unread == 0) "No new calls" else "$unread unread"
}

private fun deskCard(
    root: Container,
    x: Double,
    y: Double,
    w: Double,
    h: Double,
    fill: RGBAFill,
    title: String,
    subtitle: String,
    assets: AssetBank,
    imagePath: String?,
    onPress: () -> Unit,
) {
    val card = root.container { position(x, y) }
    card.solidRect(w, h, Palette.brass)
    val bmp = assets.get(imagePath)
    if (bmp != null) {
        card.image(bmp) {
            position(6.0, 6.0)
            scale((w - 12.0) / bmp.width.toDouble(), (h - 12.0) / bmp.height.toDouble())
        }
        card.solidRect(w - 12.0, 78.0, Palette.mahoganyDeep) {
            position(6.0, h - 84.0)
            alpha = 0.82
        }
    } else {
        card.solidRect(w - 12.0, h - 12.0, fill) { position(6.0, 6.0) }
    }
    card.text(title, textSize = 22.0, color = Palette.paper) { position(18.0, h - 72.0) }
    card.text(subtitle, textSize = 15.0, color = Palette.paperShade) { position(18.0, h - 40.0) }
    card.hitRect(w, h) { onPress() }
}

private fun openCaseDrawer(root: Container, nav: Navigator) {
    root.overlay(660.0, 720.0, dismissOnScrim = true) { dismiss ->
        text("Case Drawer", textSize = 28.0, color = Palette.ink) { position(0.0, 0.0) }
        text("Unlocked files", textSize = 16.0, color = Palette.inkSoft) { position(0.0, 40.0) }
        var y = 80.0
        val unlocked = nav.engine.progression.unlockedCaseIds
        if (unlocked.size < 2) {
            blockText(
                "The next file is still in the chief's cabinet.",
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
                if (active) append("   ·  ON THE DESK")
                if (closed) append("   ·  CLOSED")
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
                    nav.refresh()
                }
            }
            y += 100.0
        }
        actionButton("Close", 0.0, 560.0, 180.0, 52.0, Palette.brassDim, Palette.paper) { dismiss() }
    }
}
