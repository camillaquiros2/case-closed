package caseclosed.ui

import caseclosed.model.BoardNode
import caseclosed.model.CaseData
import caseclosed.model.CaseInvestigationState
import korlibs.image.color.Colors
import korlibs.korge.input.draggableCloseable
import korlibs.korge.input.onClick
import korlibs.korge.view.Container
import korlibs.korge.view.Text
import korlibs.korge.view.alpha
import korlibs.korge.view.container
import korlibs.korge.view.fixedSizeContainer
import korlibs.korge.view.image
import korlibs.korge.view.position
import korlibs.korge.view.scale
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import korlibs.math.geom.Size
import kotlin.math.hypot

private const val CARD_W = 156.0
private const val CARD_H = 72.0
private const val CORK_W = 672.0
private const val CORK_H = 520.0
private const val PAGE_SIZE = 3
private val YARN_RED = Colors["#FF2A2A"]

private data class TrayChip(
    val type: String,
    val entityId: String,
    val title: String,
    val excerpt: String,
    val image: String?,
)

fun buildBoard(root: Container, nav: Navigator) {
    val engine = nav.engine
    val case = engine.activeCase()
    root.solidRect(Desk.W, Desk.H, Palette.mahoganyDeep)
    root.chrome("Tablero", case.title, nav, true)

    var filter = "all"
    var page = 0
    var linkMode = false
    var selected: String? = null
    lateinit var drawBoard: () -> Unit
    lateinit var drawTray: () -> Unit

    fun nodeById(id: String?): BoardNode? =
        if (id == null) null else engine.investigation().boardNodes.find { it.id == id }

    fun tiedTo(nodeId: String): String {
        val state = engine.investigation()
        val names = state.boardConnections.mapNotNull { connection ->
            val otherId = when (nodeId) {
                connection.fromNodeId -> connection.toNodeId
                connection.toNodeId -> connection.fromNodeId
                else -> null
            } ?: return@mapNotNull null
            val other = state.boardNodes.find { it.id == otherId } ?: return@mapNotNull null
            clipText(displayTitle(case, state, other), 14)
        }
        return if (names.isEmpty()) "Sin atar." else "Atada a ${names.joinToString(", ")}."
    }

    fun statusText(): String {
        val state = engine.investigation()
        val count = state.boardConnections.size
        val links = when (count) {
            0 -> "Aún no hay hilos."
            1 -> "1 hilo rojo."
            else -> "$count hilos rojos."
        }
        val node = nodeById(selected)
        return if (node != null) {
            val title = clipText(displayTitle(case, state, node), 18)
            val hint = if (linkMode) "Toca otra ficha para atar o soltar." else "Toca de nuevo para leerla."
            clipText("$title. ${tiedTo(node.id)} $hint", 92)
        } else if (linkMode) {
            "Enlazando: toca dos fichas. Sin arrastre. $links"
        } else {
            "Arrastra una ficha para moverla. Tócala para leerla. $links"
        }
    }

    val status: Text = root.text("", textSize = 14.0, color = Palette.brass) {
        position(24.0, Desk.HEADER + 4.0)
    }

    val toolsY = Desk.HEADER + 28.0
    val linkBtn = root.container { position(16.0, toolsY) }
    val linkFill = linkBtn.solidRect(166.0, 46.0, Palette.wax)
    val linkLabel = linkBtn.text("Enlazar", textSize = 18.0, color = Palette.paper) { position(14.0, 12.0) }
    linkBtn.hitRect(166.0, 46.0) {
        linkMode = !linkMode
        linkFill.color = if (linkMode) Palette.gold else Palette.wax
        linkLabel.color = if (linkMode) Palette.ink else Palette.paper
        linkLabel.text = if (linkMode) "Enlazando" else "Enlazar"
        drawBoard()
    }

    fun requireSelected(missing: String): BoardNode? {
        val node = nodeById(selected)
        if (node == null) status.text = missing
        return node
    }

    root.actionButton("Quitar", 190.0, toolsY, 166.0, 46.0, Palette.wax, Palette.paper) {
        val node = requireSelected("Toca una ficha del tablero y luego Quitar.") ?: return@actionButton
        engine.removeNode(node.id)
        selected = null
        drawBoard()
        drawTray()
    }
    root.actionButton("Cortar", 364.0, toolsY, 166.0, 46.0, Palette.brassDim, Palette.paper) {
        val node = requireSelected("Toca una ficha y luego Cortar para soltar sus hilos.") ?: return@actionButton
        val doomed = engine.investigation().boardConnections.filter { it.fromNodeId == node.id || it.toNodeId == node.id }
        if (doomed.isEmpty()) {
            status.text = "Esa ficha no tiene hilos."
            return@actionButton
        }
        doomed.forEach { engine.removeConnection(it.id) }
        drawBoard()
    }
    root.actionButton("Abrir", 538.0, toolsY, 166.0, 46.0, Palette.greenFelt, Palette.paper) {
        val node = requireSelected("Toca una ficha y luego Abrir.") ?: return@actionButton
        openInspector(root, nav, case, node) {
            if (nodeById(selected) == null) selected = null
            drawBoard()
            drawTray()
        }
    }

    val corkY = Desk.HEADER + 82.0
    val cork = root.fixedSizeContainer(Size(CORK_W, CORK_H), clip = true) {
        position(24.0, corkY)
    }
    val background = cork.container()
    val nodesLayer = cork.container()
    val yarnLayer = cork.container()
    val pinsLayer = cork.container()
    yarnLayer.mouseEnabled = false
    pinsLayer.mouseEnabled = false

    val filtersY = corkY + CORK_H + 10.0
    val tabLayer = root.container()
    val trayY = filtersY + 52.0
    val trayH = Desk.H - trayY - 12.0
    root.solidRect(672.0, trayH, Palette.mahogany) { position(24.0, trayY) }
    val tray = root.fixedSizeContainer(Size(672.0, trayH), clip = true) {
        position(24.0, trayY)
    }

    fun cardTopLeft(node: BoardNode): Vec {
        val raw = Yarn.pinTopLeft(node.x, node.y, CORK_W, CORK_H)
        return Vec(
            raw.x.coerceIn(0.0, CORK_W - CARD_W),
            raw.y.coerceIn(0.0, CORK_H - CARD_H),
        )
    }

    fun pinPoint(topLeft: Vec): Vec = Vec(topLeft.x + CARD_W / 2.0, topLeft.y)

    fun boardPercent(px: Double, py: Double): Pair<Double, Double> {
        val x = (px / CORK_W * 100.0).coerceIn(0.0, (CORK_W - CARD_W) / CORK_W * 100.0)
        val y = (py / CORK_H * 100.0).coerceIn(0.0, (CORK_H - CARD_H) / CORK_H * 100.0)
        return x to y
    }

    fun redrawYarn() {
        yarnLayer.clearChildren()
        pinsLayer.clearChildren()
        val state = engine.investigation()
        val tops = state.boardNodes.associate { it.id to cardTopLeft(it) }
        for (connection in state.boardConnections) {
            val from = tops[connection.fromNodeId] ?: continue
            val to = tops[connection.toNodeId] ?: continue
            yarnLayer.yarnRope(Yarn.sample(pinPoint(from), pinPoint(to), steps = 40))
        }
        for (node in state.boardNodes) {
            val top = pinPoint(tops.getValue(node.id))
            val tack = if (node.id == selected) Palette.gold else Palette.wax
            pinsLayer.solidRect(12.0, 12.0, tack) { position(top.x - 6.0, top.y - 6.0) }
            pinsLayer.solidRect(4.0, 4.0, Palette.paper) { position(top.x - 2.0, top.y - 2.0) }
        }
    }

    fun chips(): List<TrayChip> {
        val state = engine.investigation()
        val all = buildList {
            case.suspects.forEach {
                add(TrayChip("suspect", it.id, it.name, it.shortDescription, it.portrait))
            }
            case.items.filter { it.id in state.discoveredItemIds }.forEach {
                add(TrayChip("item", it.id, it.name, it.shortNotes, it.image))
            }
            state.highlights.forEach {
                add(
                    TrayChip(
                        "highlight",
                        it.id,
                        clipText(it.text, 42),
                        it.text,
                        entityImage(case, state, "highlight", it.id),
                    ),
                )
            }
        }
        return if (filter == "all") all else all.filter { it.type == filter }
    }

    fun paintTabs() {
        tabLayer.clearChildren()
        val tabs = listOf("all" to "Todos", "suspect" to "Sospechosos", "item" to "Objetos", "highlight" to "Citas")
        tabs.forEachIndexed { index, (id, label) ->
            val x = 16.0 + index * 176.0
            tabLayer.actionButton(
                label,
                x,
                filtersY,
                168.0,
                44.0,
                if (filter == id) Palette.gold else Palette.brassDim,
                Palette.ink,
            ) {
                filter = id
                page = 0
                paintTabs()
                drawTray()
            }
        }
    }

    fun onCard(nodeId: String) {
        if (linkMode) {
            val first = selected
            if (first == null || first == nodeId) {
                selected = if (first == nodeId) null else nodeId
            } else {
                val existing = engine.investigation().boardConnections.find { connection ->
                    setOf(connection.fromNodeId, connection.toNodeId) == setOf(first, nodeId)
                }
                if (existing != null) engine.removeConnection(existing.id) else engine.connect(first, nodeId)
                selected = nodeId
            }
            drawBoard()
            return
        }
        selected = nodeId
        drawBoard()
        val node = nodeById(nodeId) ?: return
        openPreview(root, nav, case, node)
    }

    drawBoard = {
        background.clearChildren()
        nodesLayer.clearChildren()
        yarnLayer.clearChildren()
        pinsLayer.clearChildren()
        if (nodeById(selected) == null) selected = null
        val tex = nav.assets.get("images/desk/cork_texture.png")
        if (tex != null) {
            background.image(tex) {
                scale(CORK_W / tex.width.toDouble(), CORK_H / tex.height.toDouble())
            }
        } else {
            background.solidRect(CORK_W, CORK_H, Palette.cork)
            var speck = 10.0
            while (speck < CORK_H) {
                background.solidRect(CORK_W, 1.0, Palette.corkDark) {
                    position(0.0, speck)
                    alpha = 0.35
                }
                speck += 18.0
            }
        }

        val state = engine.investigation()
        val nodes = state.boardNodes
        for (node in nodes) {
            val linkCount = state.boardConnections.count { it.fromNodeId == node.id || it.toNodeId == node.id }
            val frame = when {
                node.id == selected -> Palette.gold
                linkCount > 0 -> YARN_RED
                else -> Palette.brass
            }
            placeCard(
                nodesLayer, nav, case, state, node, cardTopLeft(node), frame, linkCount,
                allowDrag = !linkMode,
                onTap = { onCard(node.id) },
                onDrag = { x, y ->
                    val (px, py) = boardPercent(x, y)
                    engine.moveNode(node.id, px, py, persist = false)
                    redrawYarn()
                },
                onDrop = { x, y ->
                    val (px, py) = boardPercent(x, y)
                    engine.moveNode(node.id, px, py, persist = true)
                    redrawYarn()
                },
            )
        }
        redrawYarn()
        status.text = statusText()
    }

    drawTray = tray@{
        tray.clearChildren()
        val list = chips()
        val pages = ((list.size + PAGE_SIZE - 1) / PAGE_SIZE).coerceAtLeast(1)
        if (page > pages - 1) page = pages - 1
        val title = when (filter) {
            "suspect" -> "Sospechosos"
            "item" -> "Objetos"
            "highlight" -> "Citas"
            else -> "Todas las pruebas"
        }
        tray.text("$title  ·  ${list.size}", textSize = 14.0, color = Palette.brass) { position(12.0, 6.0) }
        if (list.isEmpty()) {
            val empty = when (filter) {
                "item" -> "Aún no hay objetos en el casillero."
                "highlight" -> "Aún no hay citas. Marca una frase para guardar una."
                "suspect" -> "No hay sospechosos en este caso."
                else -> "Nada que fijar todavía."
            }
            tray.text(empty, textSize = 16.0, color = Palette.paper) { position(12.0, 40.0) }
            return@tray
        }
        val slice = list.drop(page * PAGE_SIZE).take(PAGE_SIZE)
        slice.forEachIndexed { index, chip ->
            val y = 30.0 + index * 122.0
            val pinned = engine.isPinned(chip.type, chip.entityId)
            val card = tray.container { position(8.0, y) }
            card.solidRect(656.0, 112.0, if (pinned) Palette.highlight else Palette.paper)
            card.solidRect(8.0, 112.0, typeTint(chip.type))
            val photo = card.container { position(16.0, 10.0) }
            photo.photo(nav.assets.get(chip.image), 68.0, 92.0, chip.title, typeTint(chip.type))
            card.solidRect(86.0, 18.0, typeTint(chip.type)) { position(96.0, 10.0) }
            card.text(typeLabel(chip.type), textSize = 12.0, color = Palette.paper) { position(104.0, 12.0) }
            card.text(clipText(chip.title, 32), textSize = 16.0, color = Palette.ink) { position(96.0, 34.0) }
            card.text(
                if (pinned) "En el tablero · toca para quitar" else "Toca para fijar al tablero",
                textSize = 13.0,
                color = if (pinned) Palette.wax else Palette.inkSoft,
            ) { position(96.0, 58.0) }
            card.text(clipText(chip.excerpt, 48), textSize = 13.0, color = Palette.inkSoft) { position(96.0, 80.0) }
            card.hitRect(656.0, 112.0) {
                val existing = engine.investigation().boardNodes.find { it.type == chip.type && it.entityId == chip.entityId }
                if (existing != null) {
                    if (selected == existing.id) selected = null
                    engine.removeNode(existing.id)
                } else {
                    engine.pin(chip.type, chip.entityId, chip.excerpt)
                }
                drawBoard()
                drawTray()
            }
        }
        if (pages > 1) {
            val pagerY = 30.0 + PAGE_SIZE * 122.0
            tray.actionButton("Anterior", 8.0, pagerY, 150.0, 44.0, Palette.brassDim, Palette.paper) {
                if (page > 0) {
                    page -= 1
                    drawTray()
                }
            }
            tray.text("${page + 1} / $pages", textSize = 16.0, color = Palette.paper) { position(290.0, pagerY + 12.0) }
            tray.actionButton("Siguiente", 498.0, pagerY, 150.0, 44.0, Palette.brassDim, Palette.paper) {
                if (page < pages - 1) {
                    page += 1
                    drawTray()
                }
            }
        }
    }

    paintTabs()
    drawBoard()
    drawTray()
}

private fun placeCard(
    layer: Container,
    nav: Navigator,
    case: CaseData,
    state: CaseInvestigationState,
    node: BoardNode,
    topLeft: Vec,
    frame: RGBAFill,
    linkCount: Int,
    allowDrag: Boolean,
    onTap: () -> Unit,
    onDrag: (Double, Double) -> Unit,
    onDrop: (Double, Double) -> Unit,
) {
    val card = layer.container { position(topLeft.x, topLeft.y) }
    card.solidRect(CARD_W, CARD_H, frame)
    card.solidRect(CARD_W - 6.0, CARD_H - 6.0, Palette.paper) { position(3.0, 3.0) }
    card.solidRect(6.0, CARD_H - 6.0, typeTint(node.type)) { position(3.0, 3.0) }
    val photo = card.container { position(12.0, 10.0) }
    val image = entityImage(case, state, node.type, node.entityId)
    photo.photo(nav.assets.get(image), 36.0, 52.0, typeLabel(node.type), typeTint(node.type))
    card.text(typeLabel(node.type), textSize = 11.0, color = typeTint(node.type)) { position(54.0, 8.0) }
    card.text(clipText(displayTitle(case, state, node), 14), textSize = 13.0, color = Palette.ink) { position(54.0, 24.0) }
    val footer = when (linkCount) {
        0 -> "Toca y lee"
        1 -> "1 hilo"
        else -> "$linkCount hilos"
    }
    card.text(footer, textSize = 11.0, color = if (linkCount > 0) Palette.wax else Palette.inkSoft) {
        position(54.0, 46.0)
    }
    val hit = card.solidRect(CARD_W, CARD_H, Colors.WHITE) { alpha = 0.01 }
    if (!allowDrag) {
        hit.onClick { onTap() }
        return
    }
    val maxX = CORK_W - CARD_W
    val maxY = CORK_H - CARD_H
    var dragged = false
    card.draggableCloseable(selector = hit, autoMove = false) { info ->
        if (info.start) dragged = false
        val nx = info.viewNextX.coerceIn(0.0, maxX)
        val ny = info.viewNextY.coerceIn(0.0, maxY)
        if (hypot(info.viewNextX - info.viewStartX, info.viewNextY - info.viewStartY) > 12.0) dragged = true
        if (dragged) card.position(nx, ny)
        if (info.end) {
            if (dragged) onDrop(nx, ny) else onTap()
        } else if (dragged) {
            onDrag(nx, ny)
        }
    }
}

private fun Container.yarnRope(points: List<Vec>) {
    for (point in points) {
        solidRect(9.0, 9.0, Colors["#4A0000"]) {
            position(point.x - 2.0, point.y - 1.0)
            mouseEnabled = false
        }
        solidRect(8.0, 8.0, YARN_RED) {
            position(point.x - 4.0, point.y - 4.0)
            mouseEnabled = false
        }
    }
}

private fun typeLabel(type: String): String = when (type) {
    "suspect" -> "Sospechoso"
    "item" -> "Objeto"
    else -> "Cita"
}

private fun typeTint(type: String): RGBAFill = when (type) {
    "suspect" -> Palette.dossier
    "item" -> Palette.locker
    else -> Palette.wax
}

private fun entityImage(case: CaseData, state: CaseInvestigationState, type: String, entityId: String): String? {
    val path = when (type) {
        "suspect" -> case.suspects.find { it.id == entityId }?.portrait
        "item" -> case.items.find { it.id == entityId }?.image
        else -> {
            val highlight = state.highlights.find { it.id == entityId } ?: return null
            when (highlight.sourceType) {
                "interview" -> case.suspects.find { it.id == highlight.sourceId }?.portrait
                "document" -> case.files.find { it.id == highlight.sourceId }?.thumbnail
                else -> null
            }
        }
    }
    return path?.takeIf { it.isNotBlank() }
}

private fun displayTitle(case: CaseData, state: CaseInvestigationState, node: BoardNode): String = when (node.type) {
    "suspect" -> case.suspects.find { it.id == node.entityId }?.name ?: "Sospechoso"
    "item" -> case.items.find { it.id == node.entityId }?.name ?: "Objeto"
    else -> {
        val text = node.selectedExcerpt ?: state.highlights.find { it.id == node.entityId }?.text.orEmpty()
        clipText(text.ifBlank { "Cita" }, 28)
    }
}

private fun fallbackExcerpt(case: CaseData, nav: Navigator, node: BoardNode): String = when (node.type) {
    "suspect" -> case.suspects.find { it.id == node.entityId }?.shortDescription.orEmpty()
    "item" -> case.items.find { it.id == node.entityId }?.shortNotes.orEmpty()
    else -> nav.engine.investigation().highlights.find { it.id == node.entityId }?.text.orEmpty()
}

private data class ExcerptChoice(val label: String, val text: String)

private fun excerptChoices(case: CaseData, nav: Navigator, node: BoardNode): List<ExcerptChoice> {
    val state = nav.engine.investigation()
    return when (node.type) {
        "suspect" -> {
            val suspect = case.suspects.find { it.id == node.entityId } ?: return emptyList()
            buildList {
                add(ExcerptChoice("Ficha oficial", suspect.shortDescription))
                add(ExcerptChoice("Coartada", suspect.alibi))
                val note = state.suspectNotes[suspect.id]
                if (!note.isNullOrBlank()) add(ExcerptChoice("Nota del detective", note))
                state.highlights
                    .filter { it.sourceType == "interview" && it.sourceId == suspect.id }
                    .forEach { add(ExcerptChoice("Testimonio", it.text)) }
            }
        }
        "item" -> {
            val item = case.items.find { it.id == node.entityId } ?: return emptyList()
            buildList {
                add(ExcerptChoice("Notas breves", item.shortNotes))
                add(ExcerptChoice("Hallazgos", item.findings))
                val note = state.itemNotes[item.id]
                if (!note.isNullOrBlank()) add(ExcerptChoice("Nota del detective", note))
            }
        }
        else -> {
            val quote = state.highlights.find { it.id == node.entityId }?.text ?: return emptyList()
            listOf(ExcerptChoice("Cita", quote))
        }
    }
}

private fun previewNotes(state: CaseInvestigationState, node: BoardNode): List<Pair<String, String>> = buildList {
    val board = node.note?.trim().orEmpty()
    if (board.isNotEmpty()) add("Nota del tablero" to board)
    when (node.type) {
        "suspect" -> state.suspectNotes[node.entityId]?.trim()?.takeIf { it.isNotEmpty() }?.let { add("Nota del expediente" to it) }
        "item" -> state.itemNotes[node.entityId]?.trim()?.takeIf { it.isNotEmpty() }?.let { add("Nota de la prueba" to it) }
        else -> state.highlights.find { it.id == node.entityId }?.note?.trim()?.takeIf { it.isNotEmpty() }?.let {
            add("Nota del extracto" to it)
        }
    }
}

private fun openPreview(host: Container, nav: Navigator, case: CaseData, node: BoardNode) {
    val state = nav.engine.investigation()
    val excerpt = node.selectedExcerpt ?: fallbackExcerpt(case, nav, node)
    host.overlay(640.0, 820.0, dismissOnScrim = true) { dismiss ->
        val photo = container()
        photo.photo(
            nav.assets.get(entityImage(case, state, node.type, node.entityId)),
            96.0,
            120.0,
            displayTitle(case, state, node),
            typeTint(node.type),
        )
        solidRect(88.0, 20.0, typeTint(node.type)) { position(112.0, 8.0) }
        text(typeLabel(node.type), textSize = 13.0, color = Palette.paper) { position(120.0, 10.0) }
        text(clipText(displayTitle(case, state, node), 26), textSize = 24.0, color = Palette.ink) {
            position(112.0, 40.0)
        }
        var y = 140.0
        text("Texto", textSize = 16.0, color = Palette.brassDim) { position(0.0, y) }
        y += 24.0
        val shown = if (excerpt.isBlank()) "Esta ficha no tiene texto." else clipText(excerpt, 420)
        y += blockText(shown, 0.0, y, 580.0, 16.0, Palette.ink)
        y += 18.0
        text("Notas", textSize = 16.0, color = Palette.brassDim) { position(0.0, y) }
        y += 24.0
        val notes = previewNotes(state, node)
        if (notes.isEmpty()) {
            blockText("Sin notas.", 0.0, y, 580.0, 16.0, Palette.inkSoft)
        } else {
            for ((label, body) in notes) {
                text(label, textSize = 14.0, color = Palette.brassDim) { position(0.0, y) }
                y += 22.0
                y += blockText(clipText(body, 320), 0.0, y, 580.0, 16.0, Palette.ink)
                y += 12.0
            }
        }
        actionButton("Cerrar", 0.0, 700.0, 180.0, 48.0, Palette.brassDim, Palette.paper) { dismiss() }
    }
}

private fun openInspector(
    host: Container,
    nav: Navigator,
    case: CaseData,
    node: BoardNode,
    onChanged: () -> Unit,
) {
    val engine = nav.engine
    val state = engine.investigation()
    host.overlay(660.0, 900.0, dismissOnScrim = true) { dismiss ->
        val photo = container()
        photo.photo(
            nav.assets.get(entityImage(case, state, node.type, node.entityId)),
            84.0,
            100.0,
            displayTitle(case, state, node),
            typeTint(node.type),
        )
        solidRect(88.0, 20.0, typeTint(node.type)) { position(100.0, 4.0) }
        text(typeLabel(node.type), textSize = 13.0, color = Palette.paper) { position(108.0, 6.0) }
        text(clipText(displayTitle(case, state, node), 28), textSize = 24.0, color = Palette.ink) {
            position(100.0, 32.0)
        }
        text("Elige la línea que se imprime en esta ficha", textSize = 14.0, color = Palette.inkSoft) {
            position(100.0, 68.0)
        }
        var y = 116.0
        for (choice in excerptChoices(case, nav, node)) {
            val row = y
            val selected = node.selectedExcerpt == choice.text
            solidRect(600.0, 64.0, if (selected) Palette.highlight else Palette.paperShade) { position(0.0, row) }
            text(choice.label, textSize = 14.0, color = Palette.brassDim) { position(10.0, row + 6.0) }
            text(clipText(choice.text, 68), textSize = 14.0, color = Palette.ink) { position(10.0, row + 30.0) }
            solidRect(600.0, 64.0, Colors.WHITE) {
                position(0.0, row)
                alpha = 0.01
                onClick {
                    engine.setNodeExcerpt(node.id, choice.text)
                    dismiss()
                    onChanged()
                }
            }
            y += 72.0
        }
        val note = node.note.orEmpty()
        blockText(
            if (note.isBlank()) "Esta ficha no tiene hipótesis." else "Hipótesis: $note",
            0.0, y, 580.0, 15.0, Palette.ink,
        )
        y += 48.0
        actionButton("Hipótesis", 0.0, y, 190.0, 48.0, Palette.brass, Palette.ink) {
            host.noteEditor("Hipótesis de la ficha", "Solo se guarda en esta ficha.", note, allowPin = false) { written, _ ->
                engine.setNodeNote(node.id, written)
                dismiss()
                onChanged()
            }
        }
        actionButton("Abrir origen", 210.0, y, 200.0, 48.0, Palette.greenFelt, Palette.paper) {
            dismiss()
            when (node.type) {
                "suspect" -> nav.push(Route.Interview(node.entityId))
                "item" -> nav.push(Route.ItemDetail(node.entityId))
                else -> {
                    val highlight = engine.investigation().highlights.find { it.id == node.entityId }
                    when (highlight?.sourceType) {
                        "document" -> nav.push(Route.Document(highlight.sourceId))
                        "interview" -> nav.push(Route.Interview(highlight.sourceId))
                    }
                }
            }
        }
        actionButton("Quitar", 430.0, y, 160.0, 48.0, Palette.wax, Palette.paper) {
            engine.removeNode(node.id)
            dismiss()
            onChanged()
        }
    }
}
