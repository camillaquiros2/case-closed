package caseclosed.ui

import caseclosed.data.ButtonIcons
import caseclosed.model.CaseData
import caseclosed.model.CaseInvestigationState
import caseclosed.model.CrimeScene
import caseclosed.model.InspectionPanelMode
import caseclosed.model.Item
import caseclosed.model.ItemOrigin
import caseclosed.model.PercentBox
import caseclosed.model.SubScene
import korlibs.image.bitmap.Bitmap
import korlibs.image.color.Colors
import korlibs.korge.input.onClick
import korlibs.korge.input.onOut
import korlibs.korge.input.onOver
import korlibs.korge.tween.get
import korlibs.korge.tween.tweenNoWait
import korlibs.korge.ui.uiTextInput
import korlibs.korge.view.Container
import korlibs.korge.view.alpha
import korlibs.korge.view.container
import korlibs.korge.view.fixedSizeContainer
import korlibs.korge.view.image
import korlibs.korge.view.position
import korlibs.korge.view.scale
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import korlibs.math.geom.Size
import korlibs.math.interpolation.EASE_OUT_QUAD
import korlibs.math.interpolation.Easing
import korlibs.time.seconds
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import korlibs.image.color.RGBA

private enum class SceneTab { PLACE, LINKS, FILES, NOTES }

private enum class SheetPage { INFO, FILE, NOTE }

private enum class ObjectFocus { MENU, LINKS, FILES }

private data class MapFrame(val x: Double, val y: Double, val w: Double, val h: Double)

private data class SceneNode(
    val key: String,
    val type: String,
    val entityId: String,
    val title: String,
    val kind: String,
)

private data class SceneEdge(
    val from: String,
    val to: String,
    val label: String,
    val connectionId: String? = null,
)

private data class SceneWeb(val nodes: List<SceneNode>, val edges: List<SceneEdge>)

private const val TAB_H = 72.0

fun buildSceneList(root: Container, nav: Navigator) {
    val case = nav.engine.activeCase()
    root.solidRect(Desk.W, Desk.H, Palette.mahoganyDeep)
    root.chrome("La escena", case.title, nav, true)
    root.scrollBox(24.0, Desk.HEADER + 16.0, 672.0, Desk.H - Desk.HEADER - 32.0, 0.0, onScroll = {}) {
        var y = 8.0
        y += blockText("Elige un lugar para registrar.", 8.0, y, 640.0, 18.0, Palette.paper)
        y += 16.0
        for (scene in case.scenes) {
            val rowY = y
            solidRect(650.0, 96.0, Palette.brass) { position(0.0, rowY) }
            solidRect(642.0, 88.0, Palette.paper) { position(4.0, rowY + 4.0) }
            text(scene.name, textSize = 24.0, color = Palette.ink) { position(20.0, rowY + 18.0) }
            text("${scene.subscenes.size} partes", textSize = 16.0, color = Palette.inkSoft) { position(20.0, rowY + 54.0) }
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
    if (scene == null) {
        missingScene(root, nav, case.title)
        return
    }
    mountScene(root, nav, scene, sub = null, initialItemId = null)
}

fun buildSceneCloseup(root: Container, nav: Navigator, sceneId: String, subSceneId: String) {
    openCloseup(root, nav, sceneId, subSceneId, null)
}

fun buildSceneFind(
    root: Container,
    nav: Navigator,
    sceneId: String,
    subSceneId: String,
    itemId: String,
) {
    openCloseup(root, nav, sceneId, subSceneId, itemId)
}

private fun openCloseup(
    root: Container,
    nav: Navigator,
    sceneId: String,
    subSceneId: String,
    itemId: String?,
) {
    val case = nav.engine.activeCase()
    val scene = case.scenes.firstOrNull { it.id == sceneId }
    val sub = scene?.subscenes?.firstOrNull { it.id == subSceneId }
    if (scene == null || sub == null) {
        missingScene(root, nav, case.title)
        return
    }
    val initial = itemId?.takeIf { id -> sub.items.any { it.itemId == id } }
    mountScene(root, nav, scene, sub, initial)
}

private fun missingScene(root: Container, nav: Navigator, title: String) {
    root.solidRect(Desk.W, Desk.H, Palette.mahoganyDeep)
    root.chrome("La escena", title, nav, true)
    root.text("Ese lugar no está en este caso.", textSize = 18.0, color = Palette.paper) {
        position(28.0, Desk.HEADER + 24.0)
    }
}

private fun mountScene(
    root: Container,
    nav: Navigator,
    scene: CrimeScene,
    sub: SubScene?,
    initialItemId: String?,
) {
    val case = nav.engine.activeCase()
    val screen = root.container()
    var selectedId = initialItemId
    var mode = InspectionPanelMode.DESCRIPTION
    var talkTo: String? = null
    var animatePanel = initialItemId != null

    fun redraw() {
        val animate = animatePanel
        animatePanel = false
        screen.clearChildren()
        screen.solidRect(Desk.W, Desk.H, Palette.mahoganyDeep)
        val title = sub?.name ?: scene.name
        val subtitle = if (sub != null) scene.name else ""
        screen.chrome(clipText(title, 24), clipText(subtitle, 48), nav, true)
        screen.drawPlace(
            root, nav, case, scene, sub,
            Desk.HEADER + 8.0,
            Desk.H - 16.0,
            selectedId,
            mode,
            talkTo,
            animate,
            onSelect = { id ->
                selectedId = if (selectedId == id) null else id
                mode = InspectionPanelMode.DESCRIPTION
                talkTo = null
                animatePanel = true
                redraw()
            },
            onAsk = { item ->
                if (mode == InspectionPanelMode.CONVERSATION) {
                    mode = InspectionPanelMode.DESCRIPTION
                    animatePanel = true
                    redraw()
                } else {
                    root.askAboutObject(nav, case, item) { suspectId ->
                        talkTo = suspectId
                        mode = InspectionPanelMode.CONVERSATION
                        animatePanel = true
                        redraw()
                    }
                }
            },
            onDocuments = {
                mode = if (mode == InspectionPanelMode.DOCUMENTS) {
                    InspectionPanelMode.DESCRIPTION
                } else {
                    InspectionPanelMode.DOCUMENTS
                }
                animatePanel = true
                redraw()
            },
            onNotes = {
                mode = if (mode == InspectionPanelMode.MENTAL_NOTES) {
                    InspectionPanelMode.DESCRIPTION
                } else {
                    InspectionPanelMode.MENTAL_NOTES
                }
                animatePanel = true
                redraw()
            },
            onClose = {
                selectedId = null
                mode = InspectionPanelMode.DESCRIPTION
                talkTo = null
                animatePanel = true
                redraw()
            },
            onRefresh = { redraw() },
        )
    }
    redraw()
}

private fun openNode(
    nav: Navigator,
    scene: CrimeScene,
    sub: SubScene?,
    node: SceneNode,
    stay: (String, SheetPage) -> Unit,
) {
    when (node.type) {
        "item", "note" -> {
            val home = scene.subscenes.firstOrNull { part -> part.items.any { it.itemId == node.entityId } }
            if (home == null) return
            val next = if (node.type == "note") SheetPage.NOTE else SheetPage.INFO
            if (sub?.id == home.id) stay(node.entityId, next)
            else nav.push(Route.SceneFind(scene.id, home.id, node.entityId))
        }
        "suspect" -> nav.push(Route.Interview(node.entityId))
        "file" -> nav.push(Route.Document(node.entityId))
    }
}

private fun Container.drawPlace(
    host: Container,
    nav: Navigator,
    case: CaseData,
    scene: CrimeScene,
    sub: SubScene?,
    top: Double,
    bottom: Double,
    selectedId: String?,
    mode: InspectionPanelMode,
    talkTo: String?,
    animate: Boolean,
    onSelect: (String) -> Unit,
    onAsk: (Item) -> Unit,
    onDocuments: () -> Unit,
    onNotes: () -> Unit,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
) {
    val selected = sub?.items?.firstOrNull { it.itemId == selectedId }?.let { spot ->
        case.items.firstOrNull { it.id == spot.itemId }
    }
    val canSend = selected != null && selected.origin == ItemOrigin.SCENE &&
        selected.id !in nav.engine.investigation().discoveredItemIds
    val memory = selected?.let { nav.engine.objectMemory(it.id) }
    val sheetH = when {
        sub == null -> 0.0
        selected == null -> 220.0
        else -> memoryPanelHeight(mode, memory?.documents?.size ?: 0, memory?.notes?.size ?: 0, canSend)
    }
    val imageBottom = bottom - sheetH - if (sub != null) 8.0 else 0.0
    val slot = MapFrame(16.0, top, Desk.W - 32.0, (imageBottom - top).coerceAtLeast(180.0))
    val frame = drawPlate(nav, if (sub == null) scene.image else sub.image, slot)
    val tip = container { mouseEnabled = false }
    fun showTip(anchorX: Double, anchorY: Double, title: String) {
        tip.clearChildren()
        val width = (title.length * 9.0 + 28.0).coerceIn(80.0, 280.0)
        val height = 36.0
        var x = anchorX + 12.0
        var y = anchorY - height - 8.0
        if (x + width > slot.x + slot.w - 8.0) x = anchorX - width - 8.0
        if (y < slot.y) y = anchorY + 28.0
        x = x.coerceIn(8.0, Desk.W - width - 8.0)
        val maxY = (imageBottom - height).coerceAtLeast(Desk.HEADER)
        y = y.coerceIn(Desk.HEADER, maxY)
        tip.position(x, y)
        tip.solidRect(width, height, Palette.brass)
        tip.solidRect(width - 6.0, height - 6.0, Palette.paper) { position(3.0, 3.0) }
        tip.text(clipText(title, 28), textSize = 16.0, color = Palette.ink) { position(12.0, 8.0) }
    }
    fun hideTip() = tip.clearChildren()
    if (sub == null) {
        scene.subscenes.forEach { part ->
            hotspot(frame, part.bounds, part.name, null, false) {
                nav.push(Route.SceneCloseup(scene.id, part.id))
            }
            val box = rectOf(frame, part.bounds)
            val hit = lastChild()
            hit?.onOver { showTip(box.x, box.y, part.name) }
            hit?.onOut { hideTip() }
        }
        iconButton(nav.assets, ButtonIcons.ESCENA, slot.x + 8.0, slot.y + slot.h - 60.0, 52.0, false) {
            host.partsDialog(nav, scene) { part ->
                nav.push(Route.SceneCloseup(scene.id, part.id))
            }
        }
    } else {
        var selectedBox: MapFrame? = null
        sub.items.forEach { spot ->
            val item = case.items.firstOrNull { it.id == spot.itemId } ?: return@forEach
            val on = item.id == selected?.id
            hotspot(frame, spot.bounds, item.name, nav.assets.get(spot.image), on) {
                onSelect(item.id)
            }
            val box = rectOf(frame, spot.bounds)
            val hit = lastChild()
            hit?.onOver { showTip(box.x, box.y, item.name) }
            hit?.onOut { hideTip() }
            if (on) selectedBox = box
        }
        val pickedBox = selectedBox
        if (selected != null && pickedBox != null) {
            drawMemoryIcons(
                nav.assets,
                frame.x, frame.y, frame.w, frame.h,
                pickedBox.x, pickedBox.y, pickedBox.w, pickedBox.h,
                mode,
                onAsk = { onAsk(selected) },
                onDocuments = onDocuments,
                onNotes = onNotes,
            )
        }
    }
    if (sub != null) {
        drawMemoryPanel(
            host, nav, case, sub.name, selected, sub.items.map { it.itemId },
            mode, talkTo, canSend,
            sheetY = imageBottom + 8.0,
            sheetH = sheetH,
            animate = animate,
            onClose = onClose,
            onRefresh = onRefresh,
        )
    }
    tip.removeFromParent()
    addChild(tip)
}

private fun Container.lastChild() = if (numChildren > 0) getChildAt(numChildren - 1) else null

private fun Container.placeActions(
    frame: MapFrame,
    box: MapFrame,
    item: Item,
    nav: Navigator,
    host: Container,
    onPage: (SheetPage) -> Unit,
    onLink: () -> Unit,
) {
    val size = 52.0
    val gap = 8.0
    val total = size * 3 + gap * 2
    var x = (box.x + box.w / 2.0 - total / 2.0).coerceIn(frame.x, frame.x + frame.w - total)
    var y = box.y - 58.0
    if (y < frame.y) y = (box.y + box.h + 6.0).coerceAtMost(frame.y + frame.h - 56.0)
    val noted = nav.engine.investigation().itemNotes[item.id].orEmpty().isNotBlank()
    iconButton(nav.assets, ButtonIcons.TAB_EXPEDIENTES, x, y, size, false) { onPage(SheetPage.FILE) }
    iconButton(nav.assets, ButtonIcons.ENLAZAR, x + size + gap, y, size, false, onLink)
    iconButton(nav.assets, ButtonIcons.NOTAS, x + (size + gap) * 2, y, size, noted) {
        val note = nav.engine.investigation().itemNotes[item.id].orEmpty()
        host.noteEditor(item.name, item.shortNotes, note, allowPin = false, assets = nav.assets) { written, _ ->
            nav.engine.setItemNote(item.id, written)
            onPage(SheetPage.INFO)
        }
    }
}

private fun Container.drawSheet(
    host: Container,
    nav: Navigator,
    case: CaseData,
    scene: CrimeScene,
    item: Item,
    focus: ObjectFocus,
    canSend: Boolean,
    sheetY: Double,
    sheetH: Double,
    onFocus: (ObjectFocus) -> Unit,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
) {
    val sheet = container { position(16.0, sheetY) }
    val width = Desk.W - 32.0
    sheet.solidRect(
        width,
        sheetH,
        Palette.panelShadow
    ) {
        position(5.0, 8.0)
        alpha = 0.55
    }
    sheet.solidRect(
        width,
        sheetH,
        Palette.brassDim
    )
    sheet.solidRect(
        width - 8.0,
        sheetH - 8.0,
        Palette.brass
    ) {
        position(4.0, 4.0)
    }
    sheet.solidRect(
        width - 16.0,
        sheetH - 16.0,
        Palette.paper
    ) {
        position(8.0, 8.0)
    }
    sheet.iconButton(nav.assets, ButtonIcons.CERRAR, width - 60.0, 12.0, 40.0, false, onClose)
    when (focus) {
        ObjectFocus.MENU -> {

            // Miniatura del objeto
            val preview = nav.assets.get(item.image)

            sheet.solidRect(
                72.0,
                72.0,
                Palette.brassDim
            ) {
                position(18.0, 16.0)
            }

            sheet.solidRect(
                64.0,
                64.0,
                Palette.paperShade
            ) {
                position(22.0, 20.0)
            }

            if (preview != null) {

                val fitted = min(
                    64.0 / preview.width.toDouble(),
                    64.0 / preview.height.toDouble()
                )

                sheet.image(preview) {
                    position(
                        22.0 + (64.0 - preview.width * fitted) / 2.0,
                        20.0 + (64.0 - preview.height * fitted) / 2.0
                    )

                    scale(fitted)
                    mouseEnabled = false
                }
            }
            sheet.text(
                clipText(item.name, 50),
                textSize = 22.0,
                color = Palette.ink
            ) {
                position(104.0, 18.0)
            }

            // Categoría
            val category = if (item.origin == ItemOrigin.SCENE) {
                "EVIDENCIA DE ESCENA"
            } else {
                "EVIDENCIA REGISTRADA"
            }

            sheet.text(
                category,
                textSize = 12.0,
                color = Palette.brassDim
            ) {
                position(104.0, 50.0)
            }
            sheet.solidRect(
                width - 36.0,
                2.0,
                Palette.brassDim
            ) {
                position(18.0, 98.0)
            }
            sheet.blockText(
                clipText(item.description, 200),
                18.0,
                110.0,
                width - 48.0,
                14.0,
                Palette.inkSoft
            )
            sheet.optionRow(
                nav.assets,
                ButtonIcons.ENLAZAR,
                "Ver conexiones",
                160.0,
                width - 36.0
            ) {
                onFocus(ObjectFocus.LINKS)
            }
            sheet.optionRow(
                nav.assets,
                ButtonIcons.TAB_EXPEDIENTES,
                "Ver expedientes",
                224.0,
                width - 36.0
            ) {
                onFocus(ObjectFocus.FILES)
            }
            if (canSend) {

                sheet.optionRow(
                    nav.assets,
                    ButtonIcons.ENVIAR,
                    "Al escritorio",
                    288.0,
                    width - 36.0
                ) {
                    nav.engine.discoverItem(item.id)
                    onRefresh()
                }
            }
        }
        ObjectFocus.LINKS -> Unit
        ObjectFocus.FILES -> {

            val vinoOscuro = RGBA(84, 27, 28, 255)
            val dorado = RGBA(181, 138, 76, 255)
            val papelClaro = RGBA(230, 210, 164, 255)
            val sombraOscura = RGBA(41, 22, 15, 255)

            sheet.optionRow(
                nav.assets,
                ButtonIcons.VOLVER,
                "Volver",
                10.0,
                180.0
            ) {
                onFocus(ObjectFocus.MENU)
            }

            val titleX = 200.0
            val titleY = 10.0
            val titleW = width - 280.0
            val titleH = 54.0

            sheet.solidRect(titleW, titleH, sombraOscura) {
                position(titleX + 2.0, titleY + 3.0)
                alpha = 0.35
                mouseEnabled = false
            }

            sheet.solidRect(titleW, titleH, dorado) {
                position(titleX, titleY)
                mouseEnabled = false
            }
            sheet.solidRect(titleW - 4.0, titleH - 4.0, vinoOscuro) {
                position(titleX + 2.0, titleY + 2.0)
                mouseEnabled = false
            }
            sheet.text(
                "ARCHIVO DE INVESTIGACIÓN",
                textSize = 13.0,
                color = papelClaro
            ) {
                position(titleX + 18.0, titleY + 7.0)
                mouseEnabled = false
            }
            sheet.text(
                "Expedientes",
                textSize = 22.0,
                color = papelClaro
            ) {
                position(titleX + 18.0, titleY + 25.0)
                mouseEnabled = false
            }

            val files = filesFor(case, item)

            sheet.scrollBox(
                16.0,
                70.0,
                width - 32.0,
                sheetH - 90.0,
                0.0,
                onScroll = {}
            ) {
                var y = 4.0

                if (files.isEmpty()) {
                    y += blockText(
                        "Sin expedientes.",
                        0.0,
                        y,
                        width - 48.0,
                        16.0,
                        Palette.inkSoft
                    )
                }
                for (file in files) {
                    val row = y
                    val rowW = width - 48.0
                    val titleLines = wrapLines(
                        file.title,
                        rowW - 76.0,
                        16.0
                    )
                    val rowH = maxOf(
                        64.0,
                        titleLines.size * 20.5 + 20.0
                    )

                    solidRect(rowW, rowH, Palette.mahoganyDeep) {
                        position(3.0, row + 4.0)
                        alpha = 0.35
                        mouseEnabled = false
                    }
                    solidRect(rowW, rowH, Palette.brassDim) {
                        position(0.0, row)
                        mouseEnabled = false
                    }
                    solidRect(rowW - 6.0, rowH - 6.0, Palette.brass) {
                        position(3.0, row + 3.0)
                        mouseEnabled = false
                    }
                    solidRect(rowW - 12.0, rowH - 12.0, Palette.paperShade) {
                        position(6.0, row + 6.0)
                        mouseEnabled = false
                    }
                    solidRect(4.0, rowH - 12.0, Palette.gold) {
                        position(6.0, row + 6.0)
                        mouseEnabled = false
                    }
                    solidRect(36.0, 42.0, Palette.brassDim) {
                        position(
                            16.0,
                            row + (rowH - 42.0) / 2.0
                        )
                        mouseEnabled = false
                    }
                    solidRect(30.0, 36.0, Palette.paper) {
                        position(
                            19.0,
                            row + (rowH - 36.0) / 2.0
                        )
                        mouseEnabled = false
                    }
                    blockText(
                        file.title,
                        62.0,
                        row + (rowH - titleLines.size * 20.5) / 2.0,
                        rowW - 82.0,
                        16.0,
                        Palette.ink
                    )
                    solidRect(rowW, rowH, Colors.WHITE) {
                        position(0.0, row)
                        alpha = 0.01
                        onClick {
                            nav.push(Route.Document(file.id))
                        }
                    }
                    y += rowH + 10.0
                }
                y
            }
        }
    }
}

private fun Container.optionRow(
    assets: AssetBank,
    icon: String,
    label: String,
    y: Double,
    width: Double,
    onPress: () -> Unit,
) {
    solidRect(width, 56.0, Palette.paperShade) { position(16.0, y) }
    iconButton(assets, icon, 24.0, y + 6.0, 44.0, false, onPress)
    text(label, textSize = 20.0, color = Palette.ink) { position(80.0, y + 16.0); mouseEnabled = false }
    solidRect(width - 64.0, 56.0, Colors.WHITE) {
        position(72.0, y)
        alpha = 0.01
        onClick { onPress() }
    }
}

private data class Spoke(
    val connectionId: String,
    val phrase: String,
    val title: String,
    val image: String?,
    val icon: String,
)

private fun objectSpokes(case: CaseData, state: CaseInvestigationState, itemId: String): List<Spoke> {
    val mine = state.boardNodes.firstOrNull { it.type == "item" && it.entityId == itemId } ?: return emptyList()
    return state.boardConnections.mapNotNull { connection ->
        val otherId = when (mine.id) {
            connection.fromNodeId -> connection.toNodeId
            connection.toNodeId -> connection.fromNodeId
            else -> return@mapNotNull null
        }
        val other = state.boardNodes.firstOrNull { it.id == otherId } ?: return@mapNotNull null
        val image = when (other.type) {
            "suspect" -> case.suspects.firstOrNull { it.id == other.entityId }?.portrait
            "item" -> case.items.firstOrNull { it.id == other.entityId }?.image
            else -> null
        }
        val icon = when (other.type) {
            "suspect" -> ButtonIcons.TAB_SOSPECHOSOS
            "item" -> ButtonIcons.TAB_OBJETOS
            "file" -> ButtonIcons.TAB_EXPEDIENTES
            else -> ButtonIcons.NOTAS
        }
        Spoke(connection.id, connection.label, nodeTitle(case, state, other.type, other.entityId), image, icon)
    }
}

private fun Container.drawLinkDialog(
    host: Container,
    nav: Navigator,
    case: CaseData,
    scene: CrimeScene,
    item: Item,
    selectedLink: String?,
    slide: Boolean,
    onPick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
) {
    val layer = container()
    val scrim = layer.solidRect(Desk.W, Desk.H, Colors.BLACK) {
        alpha = if (slide) 0.0 else 0.58
        onClick { onClose() }
    }
    val restX = 48.0
    val panelW = Desk.W - restX - 12.0
    val panelY = Desk.HEADER + 8.0
    val panelH = Desk.H - panelY - 12.0
    val panel = layer.container { position(if (slide) Desk.W else restX, panelY) }
    panel.solidRect(panelW, panelH, Palette.mahoganyDeep) {
        position(10.0, 12.0)
        alpha = 0.45
    }
    panel.solidRect(panelW, panelH, Palette.brass)
    panel.solidRect(panelW - 12.0, panelH - 12.0, Palette.paper) { position(6.0, 6.0) }
    panel.iconButton(nav.assets, ButtonIcons.VOLVER, 16.0, 16.0, 52.0, false, onClose)
    panel.text("Conexiones", textSize = 24.0, color = Palette.ink) {
        position(80.0, 16.0)
        mouseEnabled = false
    }
    panel.text(clipText(item.name, 16), textSize = 16.0, color = Palette.inkSoft) {
        position(80.0, 48.0)
        mouseEnabled = false
    }
    val size = 64.0
    val quitX = panelW - 18.0 - size
    val addX = quitX - 16.0 - size
    panel.iconButton(nav.assets, ButtonIcons.ENLAZAR, addX, 14.0, size, false) {
        host.connectionForm(nav, case, scene, item, onRefresh)
    }
    panel.text("Añadir", textSize = 15.0, color = Palette.ink) {
        position(addX + 4.0, 82.0)
        mouseEnabled = false
    }
    panel.iconButton(nav.assets, ButtonIcons.QUITAR, quitX, 14.0, size, selectedLink != null) {
        val id = selectedLink ?: return@iconButton
        onRemove(id)
    }
    panel.text("Quitar", textSize = 15.0, color = Palette.ink) {
        position(quitX + 8.0, 82.0)
        mouseEnabled = false
    }
    val spokes = objectSpokes(case, nav.engine.investigation(), item.id)
    val graphX = 16.0
    val graphY = 116.0
    val graphW = panelW - 32.0
    val graphH = panelH - 132.0
    panel.objectStar(nav, item, spokes, selectedLink, graphX, graphY, graphW, graphH, onPick)
    if (spokes.isEmpty()) {
        panel.text("Aún no hay conexiones.", textSize = 18.0, color = Palette.inkSoft) {
            position(graphX + graphW / 2.0 - 110.0, graphY + graphH / 2.0 + 108.0)
            mouseEnabled = false
        }
    }
    if (slide) {
        panel.tweenNoWait(panel::x[restX], time = 0.36.seconds, easing = Easing.EASE_OUT_QUAD) { ratio ->
            scrim.alpha = 0.58 * ratio
        }
    }
}

private fun Container.objectStar(
    nav: Navigator,
    item: Item,
    spokes: List<Spoke>,
    selectedId: String?,
    x: Double,
    y: Double,
    w: Double,
    h: Double,
    onPick: (String) -> Unit,
) {
    solidRect(w, h, Palette.paperShade) { position(x, y); mouseEnabled = false }
    val cx = x + w / 2.0
    val cy = y + h / 2.0
    val radius = min(w, h) * 0.34
    val points = spokes.mapIndexed { index, spoke ->
        val angle = -PI / 2.0 + index * (2.0 * PI / spokes.size)
        Triple(spoke, cx + cos(angle) * radius, cy + sin(angle) * radius)
    }
    for ((spoke, sx, sy) in points) {
        val selected = spoke.connectionId == selectedId
        drawPolyline(listOf(Vec(cx, cy), Vec(sx, sy)), Palette.yarnShadow, 12.0, 4.0, 5.0)
        drawPolyline(
            listOf(Vec(cx, cy), Vec(sx, sy)),
            if (selected) Palette.gold else Palette.yarn,
            if (selected) 9.0 else 7.0,
            0.0,
            0.0,
        )
    }
    for ((spoke, sx, sy) in points) {
        val selected = spoke.connectionId == selectedId
        val phrase = clipText(spoke.phrase, 18)
        val plateW = phrase.length * 10.0 + 24.0
        val plateH = 34.0
        val mx = cx + (sx - cx) * 0.58
        val my = cy + (sy - cy) * 0.58
        val dx = sx - cx
        val dy = sy - cy
        val len = hypot(dx, dy).coerceAtLeast(1.0)
        val px = (mx + (-dy / len) * 22.0 - plateW / 2.0).coerceIn(x + 6.0, x + w - plateW - 6.0)
        val py = (my + (dx / len) * 22.0 - plateH / 2.0).coerceIn(y + 6.0, y + h - plateH - 6.0)
        solidRect(plateW + 6.0, plateH + 6.0, Palette.brass) {
            position(px - 3.0, py - 3.0)
            mouseEnabled = false
        }
        solidRect(plateW, plateH, if (selected) Palette.gold else Palette.paper) {
            position(px, py)
            mouseEnabled = false
        }
        text(phrase, textSize = 18.0, color = Palette.ink) {
            position(px + 12.0, py + 7.0)
            mouseEnabled = false
        }
        solidRect(plateW, plateH, Colors.WHITE) {
            position(px, py)
            alpha = 0.01
            onClick { onPick(spoke.connectionId) }
        }
        val spokeSize = 118.0
        thumb(
            nav.assets,
            spoke.image,
            spoke.icon,
            sx - spokeSize / 2.0,
            sy - spokeSize / 2.0,
            spokeSize,
            if (selected) Palette.gold else Palette.brass,
            spoke.title,
            showCaption = true,
        ) { onPick(spoke.connectionId) }
    }
    val center = 156.0
    thumb(
        nav.assets,
        item.image,
        ButtonIcons.TAB_OBJETOS,
        cx - center / 2.0,
        cy - center / 2.0,
        center,
        Palette.gold,
        item.name,
        showCaption = true,
    ) {}
}

private data class LinkTarget(val type: String, val id: String, val title: String)

private fun linkTargets(
    case: CaseData,
    state: CaseInvestigationState,
    scene: CrimeScene,
    item: Item,
    kind: String,
): List<LinkTarget> = when (kind) {
    "item" -> (sceneItems(case, scene).map { it.id } + state.discoveredItemIds)
        .distinct()
        .filter { it != item.id }
        .mapNotNull { id ->
            case.items.firstOrNull { it.id == id }?.let { LinkTarget("item", it.id, it.name) }
        }
    "note" -> buildList {
        for ((id, note) in state.itemNotes) {
            if (note.isBlank() || id == item.id) continue
            val name = case.items.firstOrNull { it.id == id }?.name ?: continue
            add(LinkTarget("item", id, "Nota · $name"))
        }
        for ((id, note) in state.suspectNotes) {
            if (note.isBlank()) continue
            val name = case.suspects.firstOrNull { it.id == id }?.name ?: continue
            add(LinkTarget("suspect", id, "Nota · $name"))
        }
        for (highlight in state.highlights) {
            add(LinkTarget("highlight", highlight.id, clipText(highlight.text, 36)))
        }
    }
    else -> case.suspects.map { LinkTarget("suspect", it.id, it.name) }
}

private fun filesFor(case: CaseData, item: Item) =
    (item.relatedFileIds + case.files.filter { item.id in it.relatedItemIds }.map { it.id })
        .distinct()
        .mapNotNull { id -> case.files.firstOrNull { it.id == id } }

private fun Container.connectionForm(
    nav: Navigator,
    case: CaseData,
    scene: CrimeScene,
    item: Item,
    onDone: () -> Unit,
) {
    overlay(660.0, 900.0, dismissOnScrim = true) { dismiss ->
        text("Añadir conexión", textSize = 26.0, color = Palette.ink) { position(0.0, 0.0) }
        var kind = "suspect"
        var picked: LinkTarget? = null
        val choices = container()
        val list = container { position(0.0, 108.0) }
        fun paint() {
            choices.clearChildren()
            listOf("suspect" to "Persona", "item" to "Objeto", "note" to "Nota").forEachIndexed { index, (id, label) ->
                val x = index * 190.0
                choices.solidRect(176.0, 44.0, if (kind == id) Palette.gold else Palette.paperShade) { position(x, 46.0) }
                choices.text(label, textSize = 16.0, color = Palette.ink) { position(x + 40.0, 58.0); mouseEnabled = false }
                choices.solidRect(176.0, 44.0, Colors.WHITE) {
                    position(x, 46.0)
                    alpha = 0.01
                    onClick {
                        kind = id
                        picked = null
                        paint()
                    }
                }
            }
            list.clearChildren()
            val targets = linkTargets(case, nav.engine.investigation(), scene, item, kind)
            if (targets.isEmpty()) {
                list.text("Nada para unir.", textSize = 16.0, color = Palette.inkSoft)
                return
            }
            list.scrollBox(0.0, 0.0, 600.0, 280.0, 0.0, onScroll = {}) {
                var y = 0.0
                for (target in targets) {
                    val row = y
                    val on = picked?.type == target.type && picked?.id == target.id
                    solidRect(580.0, 48.0, if (on) Palette.gold else Palette.paperShade) { position(0.0, row) }
                    text(clipText(target.title, 38), textSize = 16.0, color = Palette.ink) { position(14.0, row + 14.0) }
                    solidRect(580.0, 48.0, Colors.WHITE) {
                        position(0.0, row)
                        alpha = 0.01
                        onClick {
                            picked = target
                            paint()
                        }
                    }
                    y += 56.0
                }
                y
            }
        }
        paint()
        text("Frase", textSize = 16.0, color = Palette.inkSoft) { position(0.0, 408.0) }
        val input = uiTextInput("", Size(580.0, 64.0)) {
            position(0.0, 436.0)
            textSize = 18.0
        }
        actionButton("Guardar", 0.0, 520.0, 240.0, 56.0, Palette.greenFelt, Palette.paper, nav.assets, ButtonIcons.GUARDAR) {
            val phrase = input.text.trim()
            val target = picked
            if (phrase.isBlank() || target == null) return@actionButton
            val from = nav.engine.pin("item", item.id, item.shortNotes)
            val to = nav.engine.pin(target.type, target.id, target.title)
            nav.engine.connect(from, to, phrase)
            dismiss()
            onDone()
        }
        actionButton("Cancelar", 260.0, 520.0, 200.0, 56.0, Palette.brassDim, Palette.paper, nav.assets, ButtonIcons.CANCELAR) {
            dismiss()
        }
    }
}

private data class LinkRow(val label: String, val open: () -> Unit)

private fun itemLinks(
    nav: Navigator,
    case: CaseData,
    scene: CrimeScene,
    sub: SubScene,
    item: Item,
    onSelect: (String) -> Unit,
): List<LinkRow> {
    val state = nav.engine.investigation()
    val rows = mutableListOf<LinkRow>()
    val seen = mutableSetOf<String>()
    for (fileId in item.relatedFileIds) {
        val file = case.files.firstOrNull { it.id == fileId } ?: continue
        if (seen.add("file:$fileId")) {
            rows += LinkRow("Expediente · ${file.title}") { nav.push(Route.Document(file.id)) }
        }
        for (suspectId in file.relatedSuspectIds) {
            val suspect = case.suspects.firstOrNull { it.id == suspectId } ?: continue
            if (!seen.add("suspect:$suspectId")) continue
            rows += LinkRow("Menciona · ${suspect.name}") { nav.push(Route.Interview(suspect.id)) }
        }
    }
    val mine = state.boardNodes.firstOrNull { it.type == "item" && it.entityId == item.id }
    if (mine != null) {
        for (connection in state.boardConnections) {
            val otherId = when (mine.id) {
                connection.fromNodeId -> connection.toNodeId
                connection.toNodeId -> connection.fromNodeId
                else -> continue
            }
            val other = state.boardNodes.firstOrNull { it.id == otherId } ?: continue
            val name = nodeTitle(case, state, other.type, other.entityId)
            if (!seen.add("hilo:${other.type}:${other.entityId}")) continue
            rows += LinkRow("Hilo · $name") {
                when (other.type) {
                    "suspect" -> nav.push(Route.Interview(other.entityId))
                    "item" -> {
                        val home = scene.subscenes.firstOrNull { part ->
                            part.items.any { it.itemId == other.entityId }
                        }
                        when {
                            home == null -> Unit
                            home.id == sub.id -> onSelect(other.entityId)
                            else -> nav.push(Route.SceneFind(scene.id, home.id, other.entityId))
                        }
                    }
                }
            }
        }
    }
    return rows
}

private fun Container.drawLinks(
    nav: Navigator,
    case: CaseData,
    web: SceneWeb,
    top: Double,
    bottom: Double,
    filter: String,
    zoom: Double,
    arm: String?,
    pendingFrom: String?,
    pendingTo: String?,
    pendingConnection: String?,
    onFilter: (String) -> Unit,
    onZoom: (Double) -> Unit,
    onTap: (SceneNode) -> Unit,
    onEdit: (SceneEdge) -> Unit,
    onKind: (String) -> Unit,
    onCut: () -> Unit,
) {
    val filters = listOf(
        "todos" to ButtonIcons.TAB_TODOS,
        "objetos" to ButtonIcons.TAB_OBJETOS,
        "personas" to ButtonIcons.TAB_SOSPECHOSOS,
        "expedientes" to ButtonIcons.TAB_EXPEDIENTES,
        "notas" to ButtonIcons.NOTAS,
    )
    filters.forEachIndexed { index, (id, icon) ->
        iconButton(nav.assets, icon, 16.0 + index * 56.0, top, 48.0, filter == id) { onFilter(id) }
    }
    iconButton(nav.assets, ButtonIcons.SUBIR, 560.0, top, 48.0, false) { onZoom(0.15) }
    iconButton(nav.assets, ButtonIcons.BAJAR, 616.0, top, 48.0, false) { onZoom(-0.15) }
    val choosing = pendingFrom != null && pendingTo != null
    val area = MapFrame(16.0, top + 58.0, Desk.W - 32.0, bottom - top - 58.0 - if (choosing) 68.0 else 0.0)
    val shown = filterWeb(web, filter)
    val clip = fixedSizeContainer(Size(area.w, area.h), clip = true) { position(area.x, area.y) }
    clip.solidRect(area.w, area.h, Palette.mahogany)
    val world = clip.container()
    world.scale(zoom)
    world.position(area.w * (1.0 - zoom) / 2.0, area.h * (1.0 - zoom) / 2.0)
    val spots = layoutNodes(shown.nodes, area.w, area.h)
    val nodeSize = 76.0
    for (edge in shown.edges) {
        val a = spots[edge.from] ?: continue
        val b = spots[edge.to] ?: continue
        val ax = a.x + nodeSize / 2.0
        val ay = a.y + nodeSize / 2.0
        val bx = b.x + nodeSize / 2.0
        val by = b.y + nodeSize / 2.0
        val active = edge.connectionId != null && edge.connectionId == pendingConnection
        world.drawPolyline(
            listOf(Vec(ax, ay), Vec(bx, by)),
            if (active) Palette.gold else relationColor(edge.label),
            if (edge.connectionId != null) 5.0 else 2.5,
            0.0,
            0.0,
        )
    }
    for (node in shown.nodes) {
        val at = spots[node.key] ?: continue
        val marked = node.key == arm || node.key == pendingFrom || node.key == pendingTo
        val frame = if (marked) Palette.gold else nodeFrame(node.type)
        world.thumb(
            nav.assets,
            nodeImage(case, node),
            relationIcon(node.type),
            at.x,
            at.y,
            nodeSize,
            frame,
            node.title,
        ) { onTap(node) }
    }
    for (edge in shown.edges) {
        val a = spots[edge.from] ?: continue
        val b = spots[edge.to] ?: continue
        val active = edge.connectionId != null && edge.connectionId == pendingConnection
        world.iconButton(
            nav.assets,
            relationIcon(edge.label),
            (a.x + b.x) / 2.0 + nodeSize / 2.0 - 16.0,
            (a.y + b.y) / 2.0 + nodeSize / 2.0 - 16.0,
            32.0,
            active,
        ) { onEdit(edge) }
    }
    if (choosing) {
        val kinds = listOf(
            "hilo" to ButtonIcons.ENLAZAR,
            "pertenece" to ButtonIcons.TAB_SOSPECHOSOS,
            "contiene" to ButtonIcons.TAB_OBJETOS,
            "menciona" to ButtonIcons.HIPOTESIS,
            "refuerza" to ButtonIcons.FIJAR,
        )
        val current = web.edges.firstOrNull { it.connectionId != null && it.connectionId == pendingConnection }?.label
        kinds.forEachIndexed { index, (id, icon) ->
            iconButton(nav.assets, icon, 16.0 + index * 60.0, bottom - 60.0, 52.0, current == id) { onKind(id) }
        }
        if (pendingConnection != null) {
            iconButton(nav.assets, ButtonIcons.CORTAR, 16.0 + kinds.size * 60.0, bottom - 60.0, 52.0, false, onCut)
        }
    }
}

private fun Container.drawFiles(nav: Navigator, case: CaseData, web: SceneWeb, top: Double, bottom: Double) {
    val files = web.nodes.filter { it.type == "file" }
    val size = 148.0
    scrollBox(16.0, top, Desk.W - 32.0, bottom - top, 0.0, onScroll = {}) {
        var y = 8.0
        files.chunked(4).forEach { row ->
            var x = 0.0
            for (node in row) {
                val file = case.files.firstOrNull { it.id == node.entityId }
                thumb(nav.assets, file?.thumbnail, ButtonIcons.TAB_EXPEDIENTES, x, y, size, Palette.brass, file?.title) {
                    if (file != null) nav.push(Route.Document(file.id))
                }
                x += size + 16.0
            }
            y += size + 28.0
        }
        y
    }
}

private fun Container.drawNotes(
    host: Container,
    nav: Navigator,
    case: CaseData,
    scene: CrimeScene,
    top: Double,
    bottom: Double,
    onChanged: () -> Unit,
) {
    val items = sceneItems(case, scene)
    val size = 148.0
    scrollBox(16.0, top, Desk.W - 32.0, bottom - top, 0.0, onScroll = {}) {
        var y = 8.0
        items.chunked(4).forEach { row ->
            var x = 0.0
            for (item in row) {
                val note = nav.engine.investigation().itemNotes[item.id].orEmpty()
                thumb(
                    nav.assets,
                    item.image,
                    ButtonIcons.NOTAS,
                    x,
                    y,
                    size,
                    if (note.isBlank()) Palette.brass else Palette.gold,
                    item.name,
                ) {
                    host.noteEditor(item.name, item.shortNotes, note, allowPin = false, assets = nav.assets) { written, _ ->
                        nav.engine.setItemNote(item.id, written)
                        onChanged()
                    }
                }
                x += size + 16.0
            }
            y += size + 28.0
        }
        y
    }
}

private fun Container.partsDialog(nav: Navigator, scene: CrimeScene, onPick: (SubScene) -> Unit) {
    val rows = (scene.subscenes.size + 1) / 2
    val panelH = (80.0 + rows * 300.0).coerceIn(400.0, 1040.0)
    overlay(660.0, panelH, dismissOnScrim = true) { dismiss ->
        scene.subscenes.forEachIndexed { index, part ->
            val x = (index % 2) * 300.0
            val y = (index / 2) * 280.0
            thumb(nav.assets, part.image, ButtonIcons.ESCENA, x, y, 250.0, Palette.brass, part.name, showCaption = true) {
                dismiss()
                onPick(part)
            }
        }
    }
}

private fun Container.filterDialog(current: String, onPick: (String) -> Unit) {
    val options = listOf(
        "todos" to "Todos",
        "objetos" to "Objetos",
        "personas" to "Personas",
        "expedientes" to "Expedientes",
        "notas" to "Notas",
    )
    overlay(560.0, 520.0, dismissOnScrim = true) { dismiss ->
        text("Filtrar", textSize = 26.0, color = Palette.ink) { position(0.0, 0.0) }
        var y = 52.0
        for ((id, label) in options) {
            val rowY = y
            solidRect(500.0, 56.0, if (id == current) Palette.gold else Palette.paperShade) { position(0.0, rowY) }
            text(label, textSize = 18.0, color = Palette.ink) { position(16.0, rowY + 16.0) }
            solidRect(500.0, 56.0, Colors.WHITE) {
                position(0.0, rowY)
                alpha = 0.01
                onClick {
                    dismiss()
                    onPick(id)
                }
            }
            y += 68.0
        }
    }
}

private fun Container.connectDialog(nav: Navigator, case: CaseData, origin: Item, onDone: () -> Unit) {
    val state = nav.engine.investigation()
    val sceneIds = case.scenes.flatMap { scene -> scene.subscenes.flatMap { it.items.map { spot -> spot.itemId } } }.toSet()
    data class Target(val type: String, val id: String, val label: String)
    val people = case.suspects.map { Target("suspect", it.id, it.name) }
    val things = case.items
        .filter { it.id != origin.id && (it.id in sceneIds || it.id in state.discoveredItemIds) }
        .map { Target("item", it.id, it.name) }
    overlay(660.0, 860.0, dismissOnScrim = true) { dismiss ->
        text("Nueva conexión", textSize = 26.0, color = Palette.ink) { position(0.0, 0.0) }
        blockText("Une ${origin.name} con alguien o con otro objeto. El hilo también queda en el tablero.", 0.0, 40.0, 590.0, 16.0, Palette.inkSoft)
        scrollBox(0.0, 100.0, 604.0, 680.0, 0.0, onScroll = {}) {
            var y = 0.0
            fun section(heading: String, targets: List<Target>) {
                if (targets.isEmpty()) return
                text(heading, textSize = 16.0, color = Palette.brassDim) { position(0.0, y) }
                y += 28.0
                for (target in targets) {
                    val linked = alreadyLinked(state, origin.id, target.type, target.id)
                    val rowY = y
                    solidRect(590.0, 52.0, if (linked) Palette.paper else Palette.paperShade) { position(0.0, rowY) }
                    text(
                        clipText(if (linked) "${target.label}  ·  ya unido" else target.label, 48),
                        textSize = 16.0,
                        color = Palette.ink,
                    ) { position(14.0, rowY + 14.0) }
                    if (!linked) {
                        solidRect(590.0, 52.0, Colors.WHITE) {
                            position(0.0, rowY)
                            alpha = 0.01
                            onClick {
                                val from = nav.engine.pin("item", origin.id, origin.shortNotes)
                                val excerpt = if (target.type == "suspect") {
                                    case.suspects.firstOrNull { it.id == target.id }?.shortDescription
                                } else {
                                    case.items.firstOrNull { it.id == target.id }?.shortNotes
                                }
                                val to = nav.engine.pin(target.type, target.id, excerpt)
                                nav.engine.connect(from, to)
                                dismiss()
                                onDone()
                            }
                        }
                    }
                    y += 60.0
                }
            }
            section("Personas", people)
            section("Objetos", things)
            y
        }
    }
}

private fun alreadyLinked(state: CaseInvestigationState, itemId: String, type: String, entityId: String): Boolean {
    val from = state.boardNodes.firstOrNull { it.type == "item" && it.entityId == itemId } ?: return false
    val to = state.boardNodes.firstOrNull { it.type == type && it.entityId == entityId } ?: return false
    return state.boardConnections.any { connection ->
        setOf(connection.fromNodeId, connection.toNodeId) == setOf(from.id, to.id)
    }
}

private fun Container.sceneTabs(y: Double, tabs: List<Pair<String, Boolean>>, onPick: (Int) -> Unit) {
    val gap = 8.0
    val width = (Desk.W - 32.0 - gap * (tabs.size - 1)) / tabs.size
    tabs.forEachIndexed { index, (label, on) ->
        val x = 16.0 + index * (width + gap)
        solidRect(width, 64.0, if (on) Palette.paper else Palette.mahogany) { position(x, y) }
        solidRect(width, 4.0, if (on) Palette.gold else Palette.brass) { position(x, y) }
        text(clipText(label, 18), textSize = 14.0, color = if (on) Palette.ink else Palette.paper) {
            position(x + 8.0, y + 22.0)
        }
        solidRect(width, 64.0, Colors.WHITE) {
            position(x, y)
            alpha = 0.01
            onClick { onPick(index) }
        }
    }
}

private fun Container.chip(label: String, x: Double, y: Double, w: Double, on: Boolean, press: () -> Unit) {
    solidRect(w, 40.0, if (on) Palette.gold else Palette.paper) { position(x, y) }
    text(clipText(label, 22), textSize = 14.0, color = Palette.ink) { position(x + 10.0, y + 10.0) }
    solidRect(w, 40.0, Colors.WHITE) {
        position(x, y)
        alpha = 0.01
        onClick { press() }
    }
}

private fun Container.miniButton(label: String, x: Double, y: Double, w: Double, onPress: () -> Unit) {
    solidRect(w, 40.0, Palette.brass) { position(x, y) }
    solidRect(w - 4.0, 36.0, Palette.paper) { position(x + 2.0, y + 2.0) }
    text(clipText(label, 16), textSize = 14.0, color = Palette.ink) { position(x + 8.0, y + 10.0) }
    solidRect(w, 40.0, Colors.WHITE) {
        position(x, y)
        alpha = 0.01
        onClick { onPress() }
    }
}

private fun Container.partChip(slot: MapFrame, label: String, onPress: () -> Unit) {
    val y = slot.y + slot.h - 48.0
    solidRect(230.0, 36.0, Palette.paper) { position(slot.x + 8.0, y) }
    text(label, textSize = 15.0, color = Palette.ink) { position(slot.x + 18.0, y + 8.0) }
    solidRect(230.0, 36.0, Colors.WHITE) {
        position(slot.x + 8.0, y)
        alpha = 0.01
        onClick { onPress() }
    }
}

private fun Container.hotspot(
    frame: MapFrame,
    box: PercentBox,
    label: String,
    sprite: Bitmap?,
    selected: Boolean,
    onPress: () -> Unit,
) {
    val rect = rectOf(frame, box)
    if (selected) {
        solidRect(
            rect.w + 14.0,
            rect.h + 14.0,
            Palette.goldGlow
        ) {
            position(rect.x - 7.0, rect.y - 7.0)
            alpha = 0.14
            mouseEnabled = false
        }
        solidRect(
            rect.w + 6.0,
            rect.h + 6.0,
            Palette.gold
        ) {
            position(rect.x - 3.0, rect.y - 3.0)
            alpha = 0.26
            mouseEnabled = false
        }

        // Resaltado interior
        solidRect(
            rect.w,
            rect.h,
            Palette.gold
        ) {
            position(rect.x, rect.y)
            alpha = 0.08
            mouseEnabled = false
        }
    }
    if (sprite != null) {
        val bw = sprite.width.toDouble().coerceAtLeast(1.0)
        val bh = sprite.height.toDouble().coerceAtLeast(1.0)
        val fitted = min(rect.w / bw, rect.h / bh)
        val dw = bw * fitted
        val dh = bh * fitted
        image(sprite) {
            position(rect.x + (rect.w - dw) / 2.0, rect.y + (rect.h - dh) / 2.0)
            scale(fitted)
            mouseEnabled = false
        }
    }
    val labelW = (label.length * 8.0 + 16.0)
        .coerceAtMost(rect.w)
        .coerceAtLeast(36.0)
    solidRect(
        labelW + 4.0,
        26.0,
        Palette.brassDim
    ) {
        position(rect.x - 2.0, rect.y - 2.0)
        alpha = 0.95
        mouseEnabled = false
    }
    solidRect(
        labelW,
        22.0,
        Palette.mahoganyDeep
    ) {
        position(rect.x, rect.y)
        alpha = 0.96
        mouseEnabled = false
    }
    solidRect(
        3.0,
        22.0,
        Palette.gold
    ) {
        position(rect.x, rect.y)
        mouseEnabled = false
    }
    text(clipText(label, 18), textSize = 13.0, color = Palette.paper) {
        position(rect.x + 6.0, rect.y + 3.0)
        mouseEnabled = false
    }
    if (selected) {
        solidRect(rect.w, 3.0, Palette.gold) { position(rect.x, rect.y); mouseEnabled = false }
        solidRect(rect.w, 3.0, Palette.gold) { position(rect.x, rect.y + rect.h - 3.0); mouseEnabled = false }
        solidRect(3.0, rect.h, Palette.gold) { position(rect.x, rect.y); mouseEnabled = false }
        solidRect(3.0, rect.h, Palette.gold) { position(rect.x + rect.w - 3.0, rect.y); mouseEnabled = false }
    }
    solidRect(rect.w, rect.h, Colors.WHITE) {
        position(rect.x, rect.y)
        alpha = 0.01
        onClick { onPress() }
    }
}

private fun Container.drawPlate(
    nav: Navigator,
    imagePath: String,
    slot: MapFrame
): MapFrame {
    solidRect(
        slot.w + 18.0,
        slot.h + 18.0,
        Palette.panelShadow
    ) {
        position(slot.x - 9.0, slot.y - 9.0)
        alpha = 0.65
        mouseEnabled = false
    }
    solidRect(
        slot.w + 12.0,
        slot.h + 12.0,
        Palette.brassDim
    ) {
        position(slot.x - 6.0, slot.y - 6.0)
        mouseEnabled = false
    }
    solidRect(
        slot.w + 6.0,
        slot.h + 6.0,
        Palette.brassLight
    ) {
        position(slot.x - 3.0, slot.y - 3.0)
        mouseEnabled = false
    }
    solidRect(
        slot.w,
        slot.h,
        Palette.corkDark
    ) {
        position(slot.x, slot.y)
        mouseEnabled = false
    }

    val bmp = nav.assets.get(imagePath)

    if (bmp == null) {
        return slot
    }

    val fitted = min(
        slot.w / bmp.width.toDouble(),
        slot.h / bmp.height.toDouble()
    )

    val dw = bmp.width * fitted
    val dh = bmp.height * fitted

    val x = slot.x + (slot.w - dw) / 2.0
    val y = slot.y + (slot.h - dh) / 2.0

    image(bmp) {
        position(x, y)
        scale(fitted)
        mouseEnabled = false
    }

    return MapFrame(x, y, dw, dh)
}
private fun rectOf(frame: MapFrame, box: PercentBox): MapFrame {
    val w = (frame.w * box.w / 100.0).coerceAtLeast(36.0)
    val h = (frame.h * box.h / 100.0).coerceAtLeast(36.0)
    return MapFrame(frame.x + frame.w * box.x / 100.0, frame.y + frame.h * box.y / 100.0, w, h)
}

private fun subBlurb(case: CaseData, part: SubScene): String {
    val names = part.items.mapNotNull { spot -> case.items.firstOrNull { it.id == spot.itemId }?.name }
    return if (names.isEmpty()) "Nada que recoger en esta parte." else names.joinToString(", ")
}

private fun sceneItems(case: CaseData, scene: CrimeScene): List<Item> =
    scene.subscenes
        .flatMap { it.items }
        .map { it.itemId }
        .distinct()
        .mapNotNull { id -> case.items.firstOrNull { it.id == id } }

private fun sceneWeb(case: CaseData, state: CaseInvestigationState, scene: CrimeScene): SceneWeb {
    val nodes = linkedMapOf<String, SceneNode>()
    val edges = mutableListOf<SceneEdge>()
    fun add(node: SceneNode) {
        if (node.key !in nodes) nodes[node.key] = node
    }
    fun link(from: String, to: String, label: String, connectionId: String? = null) {
        if (from == to) return
        if (connectionId != null) {
            edges.removeAll { setOf(it.from, it.to) == setOf(from, to) }
            edges += SceneEdge(from, to, label, connectionId)
            return
        }
        if (edges.any { setOf(it.from, it.to) == setOf(from, to) }) return
        edges += SceneEdge(from, to, label)
    }
    for (item in sceneItems(case, scene)) {
        add(SceneNode("item:${item.id}", "item", item.id, item.name, "Objeto"))
        val note = state.itemNotes[item.id]?.trim().orEmpty()
        if (note.isNotEmpty()) {
            add(SceneNode("note:${item.id}", "note", item.id, clipText(note, 40), "Nota"))
            link("item:${item.id}", "note:${item.id}", "Nota")
        }
        for (fileId in item.relatedFileIds) {
            val file = case.files.firstOrNull { it.id == fileId } ?: continue
            add(SceneNode("file:$fileId", "file", fileId, file.title, "Expediente"))
            link("item:${item.id}", "file:$fileId", "Expediente")
            for (suspectId in file.relatedSuspectIds) {
                val suspect = case.suspects.firstOrNull { it.id == suspectId } ?: continue
                add(SceneNode("suspect:$suspectId", "suspect", suspectId, suspect.name, "Sospechoso"))
                link("file:$fileId", "suspect:$suspectId", "Menciona")
            }
        }
    }
    for (fileNode in nodes.values.filter { it.type == "file" }) {
        val file = case.files.firstOrNull { it.id == fileNode.entityId } ?: continue
        for (itemId in file.relatedItemIds) {
            val item = case.items.firstOrNull { it.id == itemId } ?: continue
            add(SceneNode("item:$itemId", "item", itemId, item.name, "Objeto"))
            link("file:${file.id}", "item:$itemId", "Contiene")
        }
    }
    fun keyOf(type: String, entityId: String): String? = when (type) {
        "item" -> "item:$entityId"
        "suspect" -> "suspect:$entityId"
        else -> null
    }
    for (connection in state.boardConnections) {
        val from = state.boardNodes.firstOrNull { it.id == connection.fromNodeId } ?: continue
        val to = state.boardNodes.firstOrNull { it.id == connection.toNodeId } ?: continue
        val fromKey = keyOf(from.type, from.entityId) ?: continue
        val toKey = keyOf(to.type, to.entityId) ?: continue
        if (fromKey !in nodes || toKey !in nodes) continue
        link(fromKey, toKey, connection.label.ifBlank { "hilo" }, connection.id)
    }
    return SceneWeb(nodes.values.toList(), edges)
}

private fun filterWeb(web: SceneWeb, filter: String): SceneWeb {
    val type = when (filter) {
        "objetos" -> "item"
        "personas" -> "suspect"
        "expedientes" -> "file"
        "notas" -> "note"
        else -> return web
    }
    val nodes = web.nodes.filter { it.type == type }
    val keys = nodes.map { it.key }.toSet()
    return SceneWeb(nodes, web.edges.filter { it.from in keys && it.to in keys })
}

private fun layoutNodes(nodes: List<SceneNode>, width: Double, height: Double): Map<String, Vec> {
    val order = listOf("suspect", "item", "note", "file")
    val columns = order.filter { type -> nodes.any { it.type == type } }
    if (columns.isEmpty()) return emptyMap()
    val columnWidth = width / columns.size
    val placed = mutableMapOf<String, Vec>()
    for ((index, type) in columns.withIndex()) {
        val group = nodes.filter { it.type == type }
        val gap = height / (group.size + 1)
        group.forEachIndexed { row, node ->
            val x = index * columnWidth + (columnWidth - 76.0) / 2.0
            val y = (gap * (row + 1) - 38.0).coerceIn(8.0, (height - 104.0).coerceAtLeast(8.0))
            placed[node.key] = Vec(x.coerceAtLeast(4.0), y)
        }
    }
    return placed
}

private fun nodeTitle(case: CaseData, state: CaseInvestigationState, type: String, entityId: String): String =
    when (type) {
        "suspect" -> case.suspects.firstOrNull { it.id == entityId }?.name ?: "Sospechoso"
        "item" -> case.items.firstOrNull { it.id == entityId }?.name ?: "Objeto"
        else -> state.highlights.firstOrNull { it.id == entityId }?.text?.let { clipText(it, 32) } ?: "Nota"
    }

private fun Container.iconTabs(nav: Navigator, y: Double, selected: SceneTab, onPick: (SceneTab) -> Unit) {
    val tabs = listOf(
        SceneTab.PLACE to ButtonIcons.ESCENA,
        SceneTab.LINKS to ButtonIcons.ENLAZAR,
        SceneTab.FILES to ButtonIcons.TAB_EXPEDIENTES,
        SceneTab.NOTES to ButtonIcons.NOTAS,
    )
    val size = 64.0
    val gap = 18.0
    var x = (Desk.W - (tabs.size * size + (tabs.size - 1) * gap)) / 2.0
    for ((tab, icon) in tabs) {
        iconButton(nav.assets, icon, x, y + 4.0, size, tab == selected) { onPick(tab) }
        x += size + gap
    }
}

private fun Container.iconButton(
    assets: AssetBank,
    icon: String,
    x: Double,
    y: Double,
    size: Double,
    on: Boolean,
    onPress: () -> Unit,
) {
    val box = container { position(x, y) }
    box.solidRect(size, size, if (on) Palette.gold else Palette.brass)
    box.solidRect(size - 6.0, size - 6.0, if (on) Palette.wax else Palette.mahoganyDeep) {
        position(3.0, 3.0)
        mouseEnabled = false
    }
    val bmp = assets.get(icon)
    if (bmp != null) {
        val fitted = min((size - 14.0) / bmp.width.toDouble(), (size - 14.0) / bmp.height.toDouble())
        box.image(bmp) {
            scale(fitted)
            position((size - bmp.width * fitted) / 2.0, (size - bmp.height * fitted) / 2.0)
            mouseEnabled = false
        }
    }
    box.solidRect(size, size, Colors.WHITE) {
        alpha = 0.01
        onClick { onPress() }
    }
}

private fun Container.thumb(
    assets: AssetBank,
    imagePath: String?,
    fallbackIcon: String,
    x: Double,
    y: Double,
    size: Double,
    frame: RGBAFill,
    caption: String? = null,
    showCaption: Boolean = false,
    onPress: () -> Unit,
): Container {
    val box = container { position(x, y) }
    box.solidRect(size, size, frame) { mouseEnabled = false }
    val inner = size - 8.0
    val bmp = imagePath?.let { assets.get(it) }
    if (bmp != null) {
        val fitted = max(inner / bmp.width.toDouble(), inner / bmp.height.toDouble())
        box.fixedSizeContainer(Size(inner, inner), clip = true) {
            position(4.0, 4.0)
            mouseEnabled = false
            image(bmp) {
                scale(fitted)
                val dw = bmp.width * fitted
                val dh = bmp.height * fitted
                position((inner - dw) / 2.0, (inner - dh) / 2.0)
                mouseEnabled = false
            }
        }
    } else {
        val mark = assets.get(fallbackIcon)
        if (mark != null) {
            val fitted = min((size - 20.0) / mark.width.toDouble(), (size - 20.0) / mark.height.toDouble())
            box.image(mark) {
                scale(fitted)
                position((size - mark.width * fitted) / 2.0, (size - mark.height * fitted) / 2.0)
                mouseEnabled = false
            }
        }
    }
    val name = if (caption.isNullOrBlank()) {
        null
    } else {
        box.text(clipText(caption, 22), textSize = 13.0, color = if (showCaption) Palette.ink else Palette.paper) {
            position(2.0, size + 2.0)
            visible = showCaption
            mouseEnabled = false
        }
    }
    box.solidRect(size, size, Colors.WHITE) {
        alpha = 0.01
        onClick { onPress() }
        if (name != null && !showCaption) {
            onOver { name.visible = true }
            onOut { name.visible = false }
        }
    }
    return box
}

private fun relationIcon(label: String): String = when (label.lowercase()) {
    "pertenece", "suspect" -> ButtonIcons.TAB_SOSPECHOSOS
    "contiene", "item" -> ButtonIcons.TAB_OBJETOS
    "menciona" -> ButtonIcons.HIPOTESIS
    "refuerza" -> ButtonIcons.FIJAR
    "expediente", "file" -> ButtonIcons.TAB_EXPEDIENTES
    "nota", "note" -> ButtonIcons.NOTAS
    else -> ButtonIcons.ENLAZAR
}

private fun relationColor(label: String): RGBAFill = when (label.lowercase()) {
    "pertenece" -> Palette.wax
    "contiene" -> Palette.brass
    "menciona" -> Palette.gold
    "refuerza" -> Palette.greenFelt
    "expediente" -> Palette.paper
    "nota" -> Palette.gold
    else -> Palette.yarn
}

private fun nodeFrame(type: String): RGBAFill = when (type) {
    "suspect" -> Palette.wax
    "file" -> Palette.paper
    "note" -> Palette.gold
    else -> Palette.brass
}

private fun nodeImage(case: CaseData, node: SceneNode): String? = when (node.type) {
    "suspect" -> case.suspects.firstOrNull { it.id == node.entityId }?.portrait
    "item" -> case.items.firstOrNull { it.id == node.entityId }?.image
    "file" -> case.files.firstOrNull { it.id == node.entityId }?.thumbnail
    else -> null
}

private fun pinNode(nav: Navigator, case: CaseData, node: SceneNode): String? = when (node.type) {
    "suspect" -> {
        val suspect = case.suspects.firstOrNull { it.id == node.entityId } ?: return null
        nav.engine.pin("suspect", suspect.id, suspect.shortDescription)
    }
    "item" -> {
        val item = case.items.firstOrNull { it.id == node.entityId } ?: return null
        nav.engine.pin("item", item.id, item.shortNotes)
    }
    else -> null
}

private fun openFace(
    nav: Navigator,
    scene: CrimeScene,
    sub: SubScene,
    node: SceneNode,
    onSelect: (String) -> Unit,
) {
    when (node.type) {
        "file" -> nav.push(Route.Document(node.entityId))
        "suspect" -> nav.push(Route.Interview(node.entityId))
        "item", "note" -> {
            val home = scene.subscenes.firstOrNull { part -> part.items.any { it.itemId == node.entityId } }
            when {
                home == null -> Unit
                home.id == sub.id -> onSelect(node.entityId)
                else -> nav.push(Route.SceneFind(scene.id, home.id, node.entityId))
            }
        }
    }
}
