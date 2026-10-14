package caseclosed.ui

import caseclosed.model.PercentBox
import korlibs.image.color.Colors
import korlibs.korge.input.onClick
import korlibs.korge.view.Container
import korlibs.korge.view.alpha
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.position
import korlibs.korge.view.scale
import korlibs.korge.view.solidRect
import korlibs.korge.view.text

private val hotspotTints = listOf(
    Palette.highlight,
    Palette.brass,
    Palette.manila,
    Palette.gold,
    Palette.dossier,
)

private data class MapFrame(val x: Double, val y: Double, val w: Double, val h: Double)

private val mapFrame = MapFrame(24.0, Desk.HEADER + 48.0, 672.0, 520.0)

fun buildSceneList(root: Container, nav: Navigator) {
    val case = nav.engine.activeCase()
    root.solidRect(Desk.W, Desk.H, Palette.mahoganyDeep)
    root.chrome("The Scene", case.title, nav, true)
    root.scrollBox(24.0, Desk.HEADER + 16.0, 672.0, Desk.H - Desk.HEADER - 32.0, 0.0, onScroll = {}) {
        var y = 8.0
        y += blockText("Choose a place to search.", 8.0, y, 640.0, 18.0, Palette.paper)
        y += 16.0
        for (scene in case.scenes) {
            val rowY = y
            solidRect(650.0, 96.0, Palette.brass) { position(0.0, rowY) }
            solidRect(642.0, 88.0, Palette.paper) { position(4.0, rowY + 4.0) }
            text(scene.name, textSize = 24.0, color = Palette.ink) { position(20.0, rowY + 18.0) }
            text("${scene.subscenes.size} parts", textSize = 16.0, color = Palette.inkSoft) { position(20.0, rowY + 54.0) }
            solidRect(650.0, 96.0, Colors.WHITE) {
                position(0.0, rowY)
                alpha = 0.01
                onClick { nav.push(Route.SceneMap(scene.id)) }
            }
            y += 112.0
        }
        y
    }
}

fun buildSceneMap(root: Container, nav: Navigator, sceneId: String) {
    val case = nav.engine.activeCase()
    val scene = case.scenes.firstOrNull { it.id == sceneId }
    root.solidRect(Desk.W, Desk.H, Palette.mahoganyDeep)
    if (scene == null) {
        root.chrome("The Scene", case.title, nav, true)
        root.text("That place is not on this case.", textSize = 18.0, color = Palette.paper) {
            position(28.0, Desk.HEADER + 24.0)
        }
        return
    }
    root.chrome(clipText(scene.name, 22), "Tap a part of the room", nav, true)
    root.drawPlate(nav, scene.image, mapFrame)
    scene.subscenes.forEachIndexed { index, sub ->
        root.hotspot(mapFrame, sub.bounds, hotspotTints[index % hotspotTints.size], sub.name) {
            nav.push(Route.SceneCloseup(scene.id, sub.id))
        }
    }
    root.partList(
        mapFrame,
        "Parts of the room",
        scene.subscenes.map { it.name to { nav.push(Route.SceneCloseup(scene.id, it.id)) } },
    )
}

fun buildSceneCloseup(root: Container, nav: Navigator, sceneId: String, subSceneId: String) {
    val case = nav.engine.activeCase()
    val scene = case.scenes.firstOrNull { it.id == sceneId }
    val sub = scene?.subscenes?.firstOrNull { it.id == subSceneId }
    root.solidRect(Desk.W, Desk.H, Palette.mahoganyDeep)
    if (scene == null || sub == null) {
        root.chrome("The Scene", case.title, nav, true)
        root.text("That part of the room is missing.", textSize = 18.0, color = Palette.paper) {
            position(28.0, Desk.HEADER + 24.0)
        }
        return
    }
    root.chrome(clipText(sub.name, 22), scene.name, nav, true)
    root.drawPlate(nav, sub.image, mapFrame)
    if (sub.items.isEmpty()) {
        root.text("Nothing here to take to the desk.", textSize = 18.0, color = Palette.paper) {
            position(28.0, mapFrame.y + mapFrame.h + 24.0)
        }
        return
    }
    sub.items.forEachIndexed { index, spot ->
        val item = case.items.firstOrNull { it.id == spot.itemId } ?: return@forEachIndexed
        root.hotspot(mapFrame, spot.bounds, hotspotTints[index % hotspotTints.size], item.name) {
            nav.push(Route.SceneFind(scene.id, sub.id, item.id))
        }
    }
    val rows = sub.items.mapNotNull { spot ->
        val item = case.items.firstOrNull { it.id == spot.itemId } ?: return@mapNotNull null
        item.name to { nav.push(Route.SceneFind(scene.id, sub.id, item.id)) }
    }
    root.partList(mapFrame, "Objects here", rows)
}

fun buildSceneFind(
    root: Container,
    nav: Navigator,
    sceneId: String,
    subSceneId: String,
    itemId: String,
) {
    val engine = nav.engine
    val case = engine.activeCase()
    val scene = case.scenes.firstOrNull { it.id == sceneId }
    val sub = scene?.subscenes?.firstOrNull { it.id == subSceneId }
    val item = case.items.firstOrNull { it.id == itemId }
    root.solidRect(Desk.W, Desk.H, Palette.mahoganyDeep)
    if (scene == null || sub == null || item == null) {
        root.chrome("Object", case.title, nav, true)
        root.text("That object is not in this case.", textSize = 18.0, color = Palette.paper) {
            position(28.0, Desk.HEADER + 24.0)
        }
        return
    }
    val sent = item.id in engine.investigation().discoveredItemIds
    root.chrome(clipText(item.name, 22), sub.name, nav, true)
    root.scrollBox(20.0, Desk.HEADER + 8.0, 680.0, Desk.H - Desk.HEADER - 120.0, 0.0, onScroll = {}) {
        var y = 8.0
        val frame = container { position(16.0, y) }
        frame.photo(nav.assets.get(item.image), 420.0, 280.0, item.name, Palette.locker)
        y += 300.0
        text(item.name, textSize = 28.0, color = Palette.paper) { position(8.0, y) }
        y += 40.0
        y += blockText(item.description, 8.0, y, 640.0, 18.0, Palette.paper)
        y + 24.0
    }
    val barY = Desk.H - 88.0
    if (sent) {
        root.solidRect(656.0, 64.0, Palette.greenFelt) { position(32.0, barY) }
        root.text("Already sent to the desk", textSize = 20.0, color = Palette.paper) { position(48.0, barY + 18.0) }
    } else {
        root.actionButton("Send to desk", 32.0, barY, 656.0, 64.0, Palette.brass, Palette.ink) {
            engine.discoverItem(item.id)
            nav.banner = "${item.name} is on the desk."
            nav.refresh()
        }
    }
}

private fun Container.partList(frame: MapFrame, heading: String, rows: List<Pair<String, () -> Unit>>) {
    var y = frame.y + frame.h + 20.0
    text(heading, textSize = 16.0, color = Palette.brass) { position(28.0, y) }
    y += 28.0
    for ((label, onPress) in rows) {
        if (y > Desk.H - 64.0) break
        val rowY = y
        solidRect(664.0, 48.0, Palette.paper) { position(28.0, rowY) }
        text(clipText(label, 42), textSize = 18.0, color = Palette.ink) { position(42.0, rowY + 12.0) }
        solidRect(664.0, 48.0, Colors.WHITE) {
            position(28.0, rowY)
            alpha = 0.01
            onClick { onPress() }
        }
        y += 58.0
    }
}

private fun Container.drawPlate(nav: Navigator, imagePath: String, frame: MapFrame) {
    solidRect(frame.w + 12.0, frame.h + 12.0, Palette.brass) { position(frame.x - 6.0, frame.y - 6.0) }
    val bmp = nav.assets.get(imagePath)
    if (bmp != null) {
        image(bmp) {
            position(frame.x, frame.y)
            scale(frame.w / bmp.width.toDouble(), frame.h / bmp.height.toDouble())
        }
    } else {
        solidRect(frame.w, frame.h, Palette.corkDark) { position(frame.x, frame.y) }
    }
}

private fun Container.hotspot(
    frame: MapFrame,
    box: PercentBox,
    tint: RGBAFill,
    label: String,
    onPress: () -> Unit,
) {
    val x = frame.x + frame.w * box.x / 100.0
    val y = frame.y + frame.h * box.y / 100.0
    val w = (frame.w * box.w / 100.0).coerceAtLeast(28.0)
    val h = (frame.h * box.h / 100.0).coerceAtLeast(28.0)
    solidRect(w, h, tint) {
        position(x, y)
        alpha = 0.38
    }
    solidRect(w, 3.0, Palette.paper) { position(x, y) }
    val plateW = (label.length * 8.5 + 16.0).coerceAtMost(w - 8.0).coerceAtLeast(28.0)
    solidRect(plateW, 22.0, Palette.mahoganyDeep) {
        position(x + 4.0, y + 6.0)
        alpha = 0.88
    }
    text(clipText(label, 18), textSize = 14.0, color = Palette.paper) { position(x + 8.0, y + 8.0) }
    solidRect(w, h, Colors.WHITE) {
        position(x, y)
        alpha = 0.01
        onClick { onPress() }
    }
}
