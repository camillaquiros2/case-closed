package caseclosed.ui

import caseclosed.data.ButtonIcons
import caseclosed.engine.MemoryQuery
import caseclosed.model.CaseData
import caseclosed.model.HighlightRange
import caseclosed.model.InspectionPanelMode
import caseclosed.model.Item
import caseclosed.model.ReferenceType
import korlibs.image.color.Colors
import korlibs.korge.input.onClick
import korlibs.korge.input.onOut
import korlibs.korge.input.onOver
import korlibs.korge.tween.get
import korlibs.korge.tween.tweenNoWait
import korlibs.korge.ui.uiTextInput
import korlibs.korge.view.Container
import korlibs.korge.view.addUpdater
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
import kotlin.math.max
import kotlin.math.min
import kotlin.time.DurationUnit

fun memoryPanelHeight(mode: InspectionPanelMode, documents: Int, notes: Int, canSend: Boolean): Double =
    when (mode) {
        InspectionPanelMode.DESCRIPTION -> if (canSend) 360.0 else 270.0
        InspectionPanelMode.CONVERSATION -> 360.0
        InspectionPanelMode.DOCUMENTS -> (180.0 + documents.coerceAtLeast(1) * 120.0).coerceAtMost(540.0)
        InspectionPanelMode.MENTAL_NOTES -> (180.0 + notes.coerceAtLeast(1) * 140.0).coerceAtMost(560.0)
    }

fun Container.drawMemoryIcons(
    assets: AssetBank,
    frameX: Double,
    frameY: Double,
    frameW: Double,
    frameH: Double,
    boxX: Double,
    boxY: Double,
    boxW: Double,
    boxH: Double,
    mode: InspectionPanelMode,
    onAsk: () -> Unit,
    onDocuments: () -> Unit,
    onNotes: () -> Unit,
) {
    val size = 82.0
    val gap = 16.0
    val stack = size * 3 + gap * 2
    val spaceRight = frameX + frameW - (boxX + boxW)
    val spaceLeft = boxX - frameX
    val preferRight = spaceRight >= spaceLeft
    var x = if (preferRight) boxX + boxW + 10.0 else boxX - size - 10.0
    x = x.coerceIn(frameX + 4.0, frameX + frameW - size - 4.0)
    val overlaps = x < boxX + boxW && x + size > boxX
    if (overlaps) {
        x = if (preferRight) {
            (boxX - size - 10.0).coerceAtLeast(frameX + 4.0)
        } else {
            (boxX + boxW + 10.0).coerceAtMost(frameX + frameW - size - 4.0)
        }
    }
    val minY = frameY + 4.0
    val maxY = (frameY + frameH - stack - 4.0).coerceAtLeast(minY)
    val y = (boxY + (boxH - stack) / 2.0).coerceIn(minY, maxY)
    val hover = container { mouseEnabled = false }
    fun hideHelp() = hover.clearChildren()
    fun showHelp(anchorX: Double, anchorY: Double, help: String) {
        hover.clearChildren()
        val chipW = (help.length * 11.0 + 28.0).coerceIn(140.0, 360.0)
        val chipH = 40.0
        val cx = (anchorX + size / 2.0 - chipW / 2.0).coerceIn(frameX, frameX + frameW - chipW)
        var cy = anchorY - chipH - 8.0
        if (cy < frameY) cy = anchorY + size + 8.0
        hover.solidRect(chipW, chipH, Palette.brass) { position(cx, cy); mouseEnabled = false }
        hover.solidRect(chipW - 4.0, chipH - 4.0, Palette.paper) { position(cx + 2.0, cy + 2.0); mouseEnabled = false }
        hover.text(help, textSize = 18.0, color = Palette.ink) { position(cx + 12.0, cy + 8.0); mouseEnabled = false }
    }
    memoryMark(assets, x, y, size, "?", ButtonIcons.PREGUNTA, mode == InspectionPanelMode.CONVERSATION, "Preguntar a una persona", ::showHelp, ::hideHelp, onAsk)
    memoryMark(assets, x, y + size + gap, size, "▤", ButtonIcons.TAB_EXPEDIENTES, mode == InspectionPanelMode.DOCUMENTS, "Ver y leer expedientes", ::showHelp, ::hideHelp, onDocuments)
    memoryMark(assets, x, y + (size + gap) * 2, size, "◉", ButtonIcons.NOTAS, mode == InspectionPanelMode.MENTAL_NOTES, "Notas mentales", ::showHelp, ::hideHelp, onNotes)
    hover.removeFromParent()
    addChild(hover)
}

fun Container.drawMemoryPanel(
    host: Container,
    nav: Navigator,
    case: CaseData,
    placeName: String,
    item: Item?,
    sceneItemIds: List<String>,
    mode: InspectionPanelMode,
    suspectId: String?,
    canSend: Boolean,
    sheetY: Double,
    sheetH: Double,
    animate: Boolean,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
) {
    val sheet = container { position(16.0, if (animate) sheetY + 28.0 else sheetY) }
    val width = Desk.W - 32.0
    sheet.solidRect(width, sheetH, Palette.panelShadow) { position(4.0, 6.0); alpha = 0.45; mouseEnabled = false }
    sheet.solidRect(width, sheetH, Palette.brass)
    sheet.solidRect(width - 10.0, sheetH - 10.0, Palette.paper) { position(5.0, 5.0) }
    if (item == null) {
        sheet.memoryHeading("Observa $placeName", 22.0, 28.0, width - 44.0)
        sheet.text("Toca un objeto para investigar.", textSize = 22.0, color = Palette.ink) { position(22.0, 100.0) }
    } else {
        when (mode) {
            InspectionPanelMode.DESCRIPTION -> sheet.drawDescription(nav, item, canSend, width, sheetH, onClose, onRefresh)
            InspectionPanelMode.CONVERSATION -> sheet.drawConversation(nav, case, item, suspectId, width, sheetH)
            InspectionPanelMode.DOCUMENTS -> sheet.drawDocuments(host, nav, case, item, width, sheetH, onRefresh)
            InspectionPanelMode.MENTAL_NOTES -> sheet.drawNotes(host, nav, case, item, sceneItemIds, width, sheetH, onRefresh)
        }
    }
    if (animate) {
        sheet.tweenNoWait(sheet::y[sheetY], time = 0.22.seconds, easing = Easing.EASE_OUT_QUAD)
    }
}

private fun Container.drawDescription(
    nav: Navigator,
    item: Item,
    canSend: Boolean,
    width: Double,
    sheetH: Double,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
) {
    val openItem = { nav.push(Route.ItemDetail(item.id)) }
    memoryPortrait(nav.assets, item.image, 18.0, 16.0, 72.0)
    memoryHit(18.0, 16.0, 72.0, 72.0, "Ver el objeto", openItem)
    val titleW = (width - 196.0).coerceAtLeast(160.0)
    memoryHeading(clipText(item.name, 24), 104.0, 18.0, titleW)
    memoryHit(104.0, 16.0, titleW.coerceAtMost(420.0), 48.0, "Ver el objeto", openItem)
    cornerMark(width - 58.0, 16.0, "×", "Dejar el objeto") { onClose() }
    val body = wrapLines(item.description, width - 56.0, 22.0).take(4).joinToString("\n")
    blockText(body, 22.0, 108.0, width - 56.0, 22.0, Palette.ink)
    if (canSend) {
        actionButton(
            "Al escritorio",
            22.0,
            sheetH - 70.0,
            280.0,
            52.0,
            Palette.wax,
            Palette.paper,
            nav.assets,
            ButtonIcons.ENVIAR,
        ) {
            nav.engine.discoverItem(item.id)
            onRefresh()
        }
    }
}

private fun Container.drawConversation(
    nav: Navigator,
    case: CaseData,
    item: Item,
    suspectId: String?,
    width: Double,
    sheetH: Double,
) {
    val suspect = case.suspects.firstOrNull { it.id == suspectId }
    if (suspect == null) {
        text("Elige a quién preguntar.", textSize = 22.0, color = Palette.ink) { position(22.0, 36.0) }
        return
    }
    val openSuspect = { nav.push(Route.Interview(suspect.id)) }
    memoryPortrait(nav.assets, suspect.portrait, 18.0, 22.0, 108.0)
    memoryHit(18.0, 22.0, 108.0, 108.0, "Ir al interrogatorio", openSuspect)
    val nameW = (width - 180.0).coerceAtLeast(160.0)
    memoryHeading(suspect.name, 142.0, 18.0, nameW)
    memoryHit(142.0, 18.0, nameW.coerceAtMost(420.0), 48.0, "Ir al interrogatorio", openSuspect)
    val reply = MemoryQuery.reply(case, nav.engine.investigation(), suspect.id, item)
    val typed = container { position(142.0, 78.0) }
    var shown = 0
    var running = reply.isNotEmpty()
    var carry = 0.0
    val cue = text("seguir ▸", textSize = 18.0, color = Palette.wax) {
        position(width - 150.0, sheetH - 42.0)
        visible = running
    }
    fun paint() {
        typed.clearChildren()
        typed.blockText(reply.take(shown), 0.0, 0.0, width - 170.0, 22.0, Palette.ink)
        cue.visible = running
    }
    paint()
    solidRect(width - 160.0, sheetH - 90.0, Colors.WHITE) {
        position(136.0, 70.0)
        alpha = 0.01
        onClick {
            if (!running) return@onClick
            shown = reply.length
            running = false
            carry = 0.0
            paint()
        }
    }
    addUpdater { dt ->
        if (!running) return@addUpdater
        carry += dt.toDouble(DurationUnit.SECONDS) * 42.0
        val step = carry.toInt()
        if (step <= 0) return@addUpdater
        carry -= step
        shown = (shown + step).coerceAtMost(reply.length)
        if (shown >= reply.length) running = false
        paint()
    }
}

private fun Container.drawDocuments(
    host: Container,
    nav: Navigator,
    case: CaseData,
    item: Item,
    width: Double,
    sheetH: Double,
    onRefresh: () -> Unit,
) {
    memoryHeading("Expedientes", 22.0, 12.0, width - 100.0)
    cornerMark(width - 58.0, 14.0, "✎", "Ver y vincular expedientes", nav.assets, ButtonIcons.EDITAR) {
        host.documentMemoryDialog(nav, case, item, onRefresh)
    }
    val docs = nav.engine.objectMemory(item.id).documents
    if (docs.isEmpty()) {
        text("Sin registro todavía.", textSize = 20.0, color = Palette.ink) { position(22.0, 84.0) }
        blockText(
            "Con el botón editar puedes ver y vincular expedientes.",
            22.0,
            118.0,
            width - 44.0,
            20.0,
            Palette.inkSoft,
        )
        return
    }
    text("Toca la imagen o el título para leer el expediente.", textSize = 16.0, color = Palette.inkSoft) {
        position(22.0, 64.0)
        mouseEnabled = false
    }
    scrollBox(16.0, 96.0, width - 32.0, (sheetH - 112.0).coerceAtLeast(80.0), 0.0, onScroll = {}) {
        var y = 0.0
        for (link in docs) {
            val file = case.files.firstOrNull { it.id == link.documentId }
            val row = y
            val openFile = { if (file != null) nav.push(Route.Document(file.id)) }
            memoryPortrait(nav.assets, file?.thumbnail, 0.0, row, 64.0)
            memoryHit(0.0, row, 64.0, 64.0, "Leer expediente", openFile)
            val titleW = (width - 170.0).coerceAtLeast(160.0)
            memorySubtitle(file?.title ?: "Expediente", 76.0, row, titleW)
            memoryHit(76.0, row, titleW.coerceAtMost(420.0), 36.0, "Leer expediente", openFile)
            y = row + 44.0
            y += markedWords(link.fragment, link.highlights, 76.0, y, width - 150.0, null, 20.0) + 16.0
            if (y < row + 80.0) y = row + 80.0
        }
        y
    }
}

private fun Container.drawNotes(
    host: Container,
    nav: Navigator,
    case: CaseData,
    item: Item,
    sceneItemIds: List<String>,
    width: Double,
    sheetH: Double,
    onRefresh: () -> Unit,
) {
    memoryHeading("Notas mentales", 22.0, 12.0, width - 100.0)
    cornerMark(width - 58.0, 14.0, "✎", "Editar notas", nav.assets, ButtonIcons.EDITAR) {
        host.mentalNotesDialog(nav, case, item, sceneItemIds, onRefresh)
    }
    val notes = nav.engine.objectMemory(item.id).notes
    if (notes.isEmpty()) {
        text("Sin notas todavía.", textSize = 20.0, color = Palette.ink) { position(22.0, 84.0) }
        return
    }
    scrollBox(16.0, 72.0, width - 32.0, sheetH - 88.0, 0.0, onScroll = {}) {
        var y = 0.0
        for (note in notes) {
            val row = y
            val help = linkHelp(note.referenceType)
            val openNote = { openLinked(nav, note.referenceType, note.referenceId) }
            memoryPortrait(nav.assets, MemoryQuery.referenceImage(case, note.referenceType, note.referenceId), 0.0, row, 76.0)
            memoryHit(0.0, row, 76.0, 76.0, help, openNote)
            val titleW = (width - 170.0).coerceAtLeast(160.0)
            y += memorySubtitle(
                MemoryQuery.referenceName(case, note.referenceType, note.referenceId),
                88.0,
                row,
                titleW,
            )
            memoryHit(88.0, row, titleW.coerceAtMost(420.0), 36.0, help, openNote)
            y += markedWords(note.text, note.highlights, 88.0, y, width - 150.0, null, 20.0) + 20.0
            if (y < row + 88.0) y = row + 88.0
        }
        y
    }
}

private class GlassDialog(
    val inner: Container,
    val fit: (Double) -> Unit,
    val dismiss: () -> Unit,
)

private fun Container.glassDialog(panelW: Double): GlassDialog {
    val layer = container()
    layer.solidRect(Desk.W, Desk.H, Colors.BLACK) { alpha = 0.28 }
    val shell = layer.container()
    val inner = container()
    val fit: (Double) -> Unit = { contentH ->
        if (inner.parent != null) inner.removeFromParent()
        shell.clearChildren()
        val panelH = (contentH + 44.0).coerceIn(180.0, Desk.H - 32.0)
        shell.position((Desk.W - panelW) / 2.0, ((Desk.H - panelH) / 2.0).coerceAtLeast(12.0))
        shell.solidRect(panelW, panelH, Palette.brass) { alpha = 0.62 }
        shell.solidRect(panelW - 10.0, panelH - 10.0, Palette.paper) {
            position(5.0, 5.0)
            alpha = 0.82
        }
        inner.position(22.0, 18.0)
        shell.addChild(inner)
    }
    return GlassDialog(inner, fit) { layer.removeFromParent() }
}

fun Container.askAboutObject(nav: Navigator, case: CaseData, item: Item, onPick: (String) -> Unit) {
    val dialog = glassDialog(640.0)
    val listH = (case.suspects.size * 108.0).coerceAtLeast(108.0)
    val scrollH = listH.coerceAtMost(520.0)
    dialog.fit(96.0 + scrollH)
    dialog.inner.apply {
        text("¿Qué sabes sobre este objeto?", textSize = 24.0, color = Palette.ink)
        text(clipText(item.name, 36), textSize = 16.0, color = Palette.inkSoft) { position(0.0, 38.0) }
        cornerMark(540.0, 0.0, "×", "Cerrar") { dialog.dismiss() }
        scrollBox(0.0, 78.0, 580.0, scrollH, 0.0, onScroll = {}) {
            var y = 0.0
            for (suspect in case.suspects) {
                val row = y
                solidRect(560.0, 96.0, Palette.paperShade) { position(0.0, row) }
                memoryPortrait(nav.assets, suspect.portrait, 10.0, row + 12.0, 72.0)
                text(suspect.name, textSize = 20.0, color = Palette.ink) { position(98.0, row + 34.0); mouseEnabled = false }
                val help = container { mouseEnabled = false }
                solidRect(560.0, 96.0, Colors.WHITE) {
                    position(0.0, row)
                    alpha = 0.01
                    onClick {
                        dialog.dismiss()
                        onPick(suspect.id)
                    }
                    onOver {
                        help.clearChildren()
                        val label = "Preguntar a ${suspect.name}"
                        val chipW = label.length * 8.0 + 20.0
                        help.solidRect(chipW, 28.0, Palette.brass) { position(98.0, row + 62.0); mouseEnabled = false }
                        help.solidRect(chipW - 4.0, 24.0, Palette.paper) { position(100.0, row + 64.0); mouseEnabled = false }
                        help.text(label, textSize = 13.0, color = Palette.ink) { position(108.0, row + 68.0); mouseEnabled = false }
                    }
                    onOut { help.clearChildren() }
                }
                y += 108.0
            }
            y
        }
    }
}

fun Container.documentMemoryDialog(nav: Navigator, case: CaseData, item: Item, onDone: () -> Unit) {
    val dialog = glassDialog(660.0)
    val body = dialog.inner
        var page = 0
        var detailFrom = 1
        var fileId: String? = null
        var anchor: Int? = null
        var tail: Int? = null
        var marks = emptySet<Int>()
        fun close() {
            dialog.dismiss()
            onDone()
        }
        fun paint() {
            body.clearChildren()
            val memory = nav.engine.objectMemory(item.id)
            val contentH = when (page) {
                0 -> body.drawFragmentList(nav, case, item, memory.documents.size, onClose = ::close, onAdd = {
                    page = 1
                    paint()
                }, onRemove = { id ->
                    nav.engine.removeDocumentFragment(item.id, id)
                    paint()
                })
                1 -> body.drawFileChoices(
                    case,
                    onClose = ::close,
                    onBack = {
                        page = 0
                        paint()
                    },
                    onOpen = { id ->
                        fileId = id
                        anchor = null
                        tail = null
                        marks = emptySet()
                        page = 2
                        paint()
                    },
                    onRead = { id ->
                        fileId = id
                        detailFrom = 1
                        page = 3
                        paint()
                    },
                )
                2 -> {
                    val file = case.files.firstOrNull { it.id == fileId }
                    if (file == null) {
                        page = 1
                        paint()
                        return
                    }
                    val script = documentScript(file)
                    body.drawReader(
                            file,
                            script,
                            anchor,
                            tail,
                            marks,
                            onClose = ::close,
                            onBack = {
                                page = 1
                                paint()
                            },
                            onOpen = {
                                detailFrom = 2
                                page = 3
                                paint()
                            },
                            onWord = { index ->
                                val start = anchor
                                val end = tail
                                if (start == null || end == null) {
                                    if (start == null) {
                                        anchor = index
                                        marks = emptySet()
                                    } else {
                                        tail = index
                                        marks = emptySet()
                                    }
                                } else {
                                    val lo = min(start, end)
                                    val hi = max(start, end)
                                    if (index in lo..hi) {
                                        marks = if (index in marks) marks - index else marks + index
                                    } else {
                                        anchor = index
                                        tail = null
                                        marks = emptySet()
                                    }
                                }
                                paint()
                            },
                        ) {
                            val start = anchor ?: return@drawReader
                            val end = tail ?: return@drawReader
                            val lo = min(start, end)
                            val hi = max(start, end)
                            val slice = script.words.subList(lo, hi + 1)
                            val phraseStart = slice.first().start
                            val phrase = script.plain.substring(phraseStart, slice.last().end)
                            val ranges = marks.filter { it in lo..hi }.map { index ->
                                val word = script.words[index]
                                HighlightRange(word.start - phraseStart, word.end - phraseStart)
                            }
                            val id = fileId ?: return@drawReader
                            if (nav.engine.saveDocumentFragment(item.id, id, phrase, ranges)) close()
                        }
                }
                else -> {
                    val file = case.files.firstOrNull { it.id == fileId }
                    if (file == null) {
                        page = 1
                        paint()
                        return
                    }
                    body.drawFileDetail(
                        nav,
                        file,
                        onClose = ::close,
                        onBack = {
                            page = detailFrom
                            paint()
                        },
                        onMark = {
                            if (detailFrom != 2) {
                                anchor = null
                                tail = null
                                marks = emptySet()
                            }
                            page = 2
                            paint()
                        },
                        resume = detailFrom == 2,
                    )
                }
            }
            dialog.fit(contentH)
        }
        paint()
}

fun Container.mentalNotesDialog(
    nav: Navigator,
    case: CaseData,
    item: Item,
    sceneItemIds: List<String>,
    onDone: () -> Unit,
) {
    val dialog = glassDialog(660.0)
    val body = dialog.inner
        var page = 0
        var editingId: String? = null
        var kind = ReferenceType.OBJECT
        var pickedId: String? = null
        var draft = ""
        var marking = false
        var highlights = emptyList<HighlightRange>()
        fun close() {
            dialog.dismiss()
            onDone()
        }
        fun targets(): List<Pair<String, String>> = when (kind) {
            ReferenceType.PERSON -> case.suspects.map { it.id to it.name }
            ReferenceType.OBJECT -> (sceneItemIds + nav.engine.investigation().discoveredItemIds)
                .distinct()
                .mapNotNull { id -> case.items.firstOrNull { it.id == id }?.let { found -> found.id to found.name } }
            ReferenceType.DOCUMENT -> case.files.map { it.id to it.title }
        }
        fun paint() {
            body.clearChildren()
            val contentH = if (page == 0) {
                body.drawNoteList(nav, case, item, onClose = ::close, onAdd = {
                    editingId = null
                    kind = ReferenceType.OBJECT
                    pickedId = null
                    draft = ""
                    marking = false
                    highlights = emptyList()
                    page = 1
                    paint()
                }, onEdit = { noteId ->
                    val note = nav.engine.objectMemory(item.id).notes.firstOrNull { it.id == noteId }
                    if (note != null) {
                        editingId = note.id
                        kind = note.referenceType
                        pickedId = note.referenceId
                        draft = note.text
                        highlights = note.highlights
                        marking = true
                        page = 1
                        paint()
                    }
                }, onRemove = { noteId ->
                    nav.engine.removeMentalNote(item.id, noteId)
                    paint()
                })
            } else {
                body.drawNoteEditor(
                    nav, case, item, kind, pickedId, targets(), draft, marking, highlights,
                    onClose = ::close,
                    onBack = { page = 0; paint() },
                    onKind = {
                        kind = it
                        pickedId = null
                        paint()
                    },
                    onPick = { pickedId = it; paint() },
                    onDraft = { draft = it },
                    onMark = {
                        draft = it
                        marking = true
                        if (highlights.any { range -> range.endExclusive > draft.length }) highlights = emptyList()
                        paint()
                    },
                    onRewrite = { marking = false; paint() },
                    onToggle = { start, end ->
                        highlights = if (highlights.any { it.start == start && it.endExclusive == end }) {
                            highlights.filterNot { it.start == start && it.endExclusive == end }
                        } else {
                            highlights + HighlightRange(start, end)
                        }
                        paint()
                    },
                    onSave = { text, ranges ->
                        val ref = pickedId
                        if (ref != null && nav.engine.saveMentalNote(item.id, editingId, kind, ref, text, ranges)) {
                            close()
                        }
                    },
                )
            }
            dialog.fit(contentH)
        }
        paint()
}

private fun Container.drawFragmentList(
    nav: Navigator,
    case: CaseData,
    item: Item,
    count: Int,
    onClose: () -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
): Double {
    text("Expedientes vinculados", textSize = 26.0, color = Palette.ink)
    text("${item.name} · $count/${MemoryQuery.LIMIT}", textSize = 18.0, color = Palette.inkSoft) { position(0.0, 36.0) }
    cornerMark(560.0, 0.0, "×", "Cerrar") { onClose() }
    val docs = nav.engine.objectMemory(item.id).documents
    var y = 78.0
    if (docs.isEmpty()) {
        text("No hay registros todavía.", textSize = 18.0, color = Palette.inkSoft) { position(0.0, y) }
        y += 44.0
    }
    for (link in docs) {
        val row = y
        val title = case.files.firstOrNull { it.id == link.documentId }?.title ?: "Expediente"
        solidRect(580.0, 88.0, Palette.paperShade) { position(0.0, row); alpha = 0.72 }
        text(clipText(title, 32), textSize = 16.0, color = Palette.inkSoft) { position(12.0, row + 8.0) }
        text(clipText(link.fragment, 48), textSize = 18.0, color = Palette.ink) { position(12.0, row + 36.0) }
        cornerMark(524.0, row + 24.0, "×", "Quitar fragmento") { onRemove(link.id) }
        y += 100.0
    }
    val full = count >= MemoryQuery.LIMIT
    val label = if (full) "Límite de cuatro" else "+ Añadir expediente"
    actionButton(label, 0.0, y + 8.0, 420.0, 56.0, if (full) Palette.brassDim else Palette.wax, Palette.paper, nav.assets, ButtonIcons.ABRIR) {
        if (!full) onAdd()
    }
    return y + 76.0
}

private fun Container.drawFileChoices(
    case: CaseData,
    onClose: () -> Unit,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onRead: (String) -> Unit,
): Double {
    text("Elegir expediente", textSize = 26.0, color = Palette.ink)
    cornerMark(0.0, 44.0, "‹", "Volver") { onBack() }
    cornerMark(560.0, 0.0, "×", "Cerrar") { onClose() }
    val listH = (case.files.size * 84.0).coerceAtLeast(84.0)
    val scrollH = listH.coerceAtMost(480.0)
    scrollBox(0.0, 100.0, 600.0, scrollH, 0.0, onScroll = {}) {
        var y = 0.0
        for (file in case.files) {
            val row = y
            solidRect(580.0, 72.0, Palette.paperShade) { position(0.0, row) }
            text(clipText(file.title, 28), textSize = 20.0, color = Palette.ink) { position(14.0, row + 10.0); mouseEnabled = false }
            text("Marcar una frase", textSize = 16.0, color = Palette.mahoganyDeep) { position(14.0, row + 40.0); mouseEnabled = false }
            solidRect(430.0, 72.0, Colors.WHITE) {
                position(0.0, row)
                alpha = 0.01
                onClick { onOpen(file.id) }
            }
            solidRect(124.0, 48.0, Palette.brassDim) { position(444.0, row + 12.0) }
            text("Ver", textSize = 18.0, color = Palette.paper) { position(486.0, row + 24.0); mouseEnabled = false }
            val help = container { mouseEnabled = false }
            solidRect(124.0, 48.0, Colors.WHITE) {
                position(444.0, row + 12.0)
                alpha = 0.01
                onClick { onRead(file.id) }
                onOver {
                    help.clearChildren()
                    help.solidRect(168.0, 28.0, Palette.brass) { position(400.0, row + 64.0); mouseEnabled = false }
                    help.solidRect(164.0, 24.0, Palette.paper) { position(402.0, row + 66.0); mouseEnabled = false }
                    help.text("Ver el expediente", textSize = 13.0, color = Palette.ink) { position(410.0, row + 70.0); mouseEnabled = false }
                }
                onOut { help.clearChildren() }
            }
            y += 84.0
        }
        y
    }
    return 112.0 + scrollH
}

private fun Container.drawFileDetail(
    nav: Navigator,
    file: caseclosed.model.CaseFile,
    onClose: () -> Unit,
    onBack: () -> Unit,
    onMark: () -> Unit,
    resume: Boolean,
): Double {
    memoryHeading(clipText(file.title, 26), 0.0, 0.0, 500.0)
    text("${file.documentNumber}    ${file.date}", textSize = 16.0, color = Palette.inkSoft) { position(0.0, 50.0) }
    cornerMark(0.0, 78.0, "‹", "Volver") { onBack() }
    cornerMark(560.0, 0.0, "×", "Cerrar") { onClose() }
    val bodyH = fileDetailHeight(nav, file).coerceIn(140.0, 460.0)
    scrollBox(0.0, 130.0, 600.0, bodyH, 0.0, onScroll = {}) {
        var y = 4.0
        val saved = nav.engine.investigation().highlights.filter { it.sourceId == file.id }
        if (file.shortDescription.isNotBlank()) {
            y += annotatedProse(file.shortDescription, 8.0, y, 540.0, 18.0)
            y += 12.0
        }
        for (section in file.sections) {
            val sectionNotes = saved.filter { it.sectionId == section.id || it.sectionId == null }
            val marks = section.highlightableClues.orEmpty().map { it.text } + sectionNotes.map { it.text }
            val notes = sectionNotes.filter { it.note.isNotBlank() }.associate { it.text to it.note }
            if (section.heading.isNotBlank()) y += penHeading(section.heading, 0.0, y, 520.0)
            if (section.content.isNotBlank()) {
                y += annotatedProse(section.content, 16.0, y, 540.0, 20.0, marks, notes)
                y += 12.0
            }
            val image = section.image
            if (image != null) {
                val shot = container { position(0.0, y) }
                shot.photo(nav.assets.get(image.src), 420.0, 220.0, image.caption, Palette.paperShade)
                y += 232.0
                if (image.caption.isNotBlank()) {
                    y += blockText(image.caption, 0.0, y, 560.0, 16.0, Palette.inkSoft)
                    y += 12.0
                }
            }
        }
        if (y < 40.0) {
            text("Este expediente no tiene texto.", textSize = 20.0, color = Palette.ink) { position(0.0, y) }
            y = 48.0
        }
        y
    }
    val buttonsY = 146.0 + bodyH
    val label = if (resume) "Volver a la frase" else "Marcar una frase"
    actionButton(label, 0.0, buttonsY, 340.0, 56.0, Palette.wax, Palette.paper, nav.assets, ButtonIcons.ABRIR, onMark)
    return buttonsY + 68.0
}

private fun fileDetailHeight(nav: Navigator, file: caseclosed.model.CaseFile): Double {
    val saved = nav.engine.investigation().highlights.filter { it.sourceId == file.id }
    var y = 4.0
    if (file.shortDescription.isNotBlank()) {
        y += inkProseHeight(file.shortDescription, 540.0, 18.0)
        y += 12.0
    }
    for (section in file.sections) {
        val sectionNotes = saved.filter { it.sectionId == section.id || it.sectionId == null }
        val marks = section.highlightableClues.orEmpty().map { it.text } + sectionNotes.map { it.text }
        val notes = sectionNotes.filter { it.note.isNotBlank() }.associate { it.text to it.note }
        if (section.heading.isNotBlank()) y += 48.0
        if (section.content.isNotBlank()) {
            y += inkProseHeight(section.content, 540.0, 20.0, marks, notes)
            y += 12.0
        }
        if (section.image != null) {
            y += 232.0
            val caption = section.image.caption
            if (caption.isNotBlank()) y += wrapLines(caption, 560.0, 16.0).size * 16.0 * 1.28 + 12.0
        }
    }
    return y.coerceAtLeast(48.0)
}

private fun Container.drawReader(
    file: caseclosed.model.CaseFile,
    script: DocumentScript,
    anchor: Int?,
    tail: Int?,
    marks: Set<Int>,
    onClose: () -> Unit,
    onBack: () -> Unit,
    onOpen: () -> Unit,
    onWord: (Int) -> Unit,
    onSave: () -> Unit,
): Double {
    memoryHeading(clipText(file.title, 28), 0.0, 0.0, 520.0)
    blockText(
        "Toca el inicio y el final de la frase. Dentro de ella, toca palabras para sobresaltarlas.",
        0.0,
        52.0,
        520.0,
        18.0,
        Palette.ink,
    )
    cornerMark(0.0, 108.0, "‹", "Volver") { onBack() }
    cornerMark(560.0, 0.0, "×", "Cerrar") { onClose() }
    val phrase = when {
        anchor != null && tail != null -> min(anchor, tail)..max(anchor, tail)
        anchor != null -> anchor..anchor
        else -> null
    }
    val bodyH = scriptBodyHeight(script).coerceIn(80.0, 480.0)
    val hint: (Int) -> String = { index ->
        val start = anchor
        val end = tail
        when {
            start == null -> "Selecciona el inicio"
            end == null -> "Selecciona el final"
            index in minOf(start, end)..maxOf(start, end) -> "Se puede resaltar"
            else -> "Selecciona el inicio"
        }
    }
    scrollBox(0.0, 160.0, 600.0, bodyH, 0.0, onScroll = {}) {
        val tips = container { mouseEnabled = false }
        var y = 4.0
        if (script.words.isEmpty()) {
            text("Este expediente no tiene texto.", textSize = 20.0, color = Palette.ink)
            y = 40.0
        }
        var wordIndex = 0
        for (block in script.blocks) {
            if (block.heading.isNotBlank()) y += sectionHeading(block.heading, 0.0, y, 560.0)
            val count = block.words.size
            y += flowPhrase(
                block.words,
                wordIndex,
                phrase,
                marks,
                0.0,
                y,
                560.0,
                onWord,
                tips,
                hint,
            )
            wordIndex += count
            y += 22.0
        }
        tips.removeFromParent()
        addChild(tips)
        y
    }
    val ready = anchor != null && tail != null
    val buttonsY = 172.0 + bodyH
    solidRect(280.0, 56.0, if (ready) Palette.greenFelt else Palette.brassDim) { position(0.0, buttonsY) }
    text("Guardar fragmento", textSize = 18.0, color = Palette.paper) { position(36.0, buttonsY + 16.0); mouseEnabled = false }
    solidRect(280.0, 56.0, Colors.WHITE) {
        position(0.0, buttonsY)
        alpha = 0.01
        onClick { if (ready) onSave() }
    }
    solidRect(250.0, 56.0, Palette.brassDim) { position(300.0, buttonsY) }
    text("Ver expediente", textSize = 18.0, color = Palette.paper) { position(328.0, buttonsY + 16.0); mouseEnabled = false }
    solidRect(250.0, 56.0, Colors.WHITE) {
        position(300.0, buttonsY)
        alpha = 0.01
        onClick { onOpen() }
    }
    return buttonsY + 68.0
}

private fun Container.drawNoteList(
    nav: Navigator,
    case: CaseData,
    item: Item,
    onClose: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onRemove: (String) -> Unit,
): Double {
    val notes = nav.engine.objectMemory(item.id).notes
    text("Notas mentales", textSize = 26.0, color = Palette.ink)
    text("${notes.size}/${MemoryQuery.LIMIT}", textSize = 18.0, color = Palette.inkSoft) { position(0.0, 36.0) }
    cornerMark(560.0, 0.0, "×", "Cerrar") { onClose() }
    var y = 78.0
    if (notes.isEmpty()) {
        text("No hay notas todavía.", textSize = 18.0, color = Palette.inkSoft) { position(0.0, y) }
        y += 44.0
    }
    for (note in notes) {
        val row = y
        solidRect(580.0, 110.0, Palette.paperShade) { position(0.0, row); alpha = 0.72 }
        memoryPortrait(nav.assets, MemoryQuery.referenceImage(case, note.referenceType, note.referenceId), 8.0, row + 16.0, 72.0)
        text(clipText(MemoryQuery.referenceName(case, note.referenceType, note.referenceId), 22), textSize = 16.0, color = Palette.inkSoft) {
            position(92.0, row + 10.0)
        }
        text(clipText(note.text, 42), textSize = 18.0, color = Palette.ink) { position(92.0, row + 38.0) }
            cornerMark(430.0, row + 58.0, "✎", "Editar nota", nav.assets, ButtonIcons.EDITAR) { onEdit(note.id) }
        cornerMark(500.0, row + 58.0, "×", "Quitar nota") { onRemove(note.id) }
        y += 122.0
    }
    val full = notes.size >= MemoryQuery.LIMIT
    actionButton(
        if (full) "Límite de cuatro" else "+ Añadir nota",
        0.0,
        y + 8.0,
        360.0,
        56.0,
        if (full) Palette.brassDim else Palette.wax,
        Palette.paper,
        nav.assets,
        ButtonIcons.NOTAS,
    ) {
        if (!full) onAdd()
    }
    return y + 76.0
}

private fun Container.drawNoteEditor(
    nav: Navigator,
    case: CaseData,
    item: Item,
    kind: ReferenceType,
    pickedId: String?,
    targets: List<Pair<String, String>>,
    draft: String,
    marking: Boolean,
    highlights: List<HighlightRange>,
    onClose: () -> Unit,
    onBack: () -> Unit,
    onKind: (ReferenceType) -> Unit,
    onPick: (String) -> Unit,
    onDraft: (String) -> Unit,
    onMark: (String) -> Unit,
    onRewrite: () -> Unit,
    onToggle: (Int, Int) -> Unit,
    onSave: (String, List<HighlightRange>) -> Unit,
): Double {
    text("Nota sobre ${clipText(item.name, 24)}", textSize = 22.0, color = Palette.ink)
    cornerMark(0.0, 36.0, "‹", "Volver") { onBack() }
    cornerMark(560.0, 0.0, "×", "Cerrar") { onClose() }
    val field = if (!marking) {
        uiTextInput(draft, Size(580.0, 90.0)) {
            position(0.0, 418.0)
            textSize = 16.0
        }
    } else {
        null
    }
    listOf(
        ReferenceType.OBJECT to "Objeto",
        ReferenceType.PERSON to "Persona",
        ReferenceType.DOCUMENT to "Expediente",
    ).forEachIndexed { index, (type, label) ->
        val x = index * 196.0
        solidRect(188.0, 40.0, if (kind == type) Palette.gold else Palette.paperShade) { position(x, 78.0) }
        text(label, textSize = 16.0, color = Palette.ink) { position(x + 46.0, 88.0); mouseEnabled = false }
        solidRect(188.0, 40.0, Colors.WHITE) {
            position(x, 78.0)
            alpha = 0.01
            onClick {
                if (field != null) onDraft(field.text)
                onKind(type)
            }
        }
    }
    val picked = pickedId?.let { id ->
        MemoryQuery.referenceName(case, kind, id) to MemoryQuery.referenceImage(case, kind, id)
    }
    if (picked != null) {
        memoryPortrait(nav.assets, picked.second, 0.0, 132.0, 56.0)
        text(clipText(picked.first, 32), textSize = 16.0, color = Palette.ink) { position(68.0, 150.0) }
    }
    scrollBox(0.0, 200.0, 600.0, 180.0, 0.0, onScroll = {}) {
        var y = 0.0
        for ((id, title) in targets) {
            val row = y
            val on = id == pickedId
            solidRect(580.0, 44.0, if (on) Palette.gold else Palette.paperShade) { position(0.0, row) }
            text(clipText(title, 40), textSize = 15.0, color = Palette.ink) { position(12.0, row + 12.0); mouseEnabled = false }
            solidRect(580.0, 44.0, Colors.WHITE) {
                position(0.0, row)
                alpha = 0.01
                onClick {
                    if (field != null) onDraft(field.text)
                    onPick(id)
                }
            }
            y += 52.0
        }
        y
    }
    text("Escribe tu pensamiento", textSize = 14.0, color = Palette.inkSoft) { position(0.0, 392.0) }
    if (field != null) {
        solidRect(260.0, 48.0, Palette.brassDim) { position(0.0, 522.0) }
        text("Marcar palabras", textSize = 16.0, color = Palette.paper) { position(48.0, 536.0); mouseEnabled = false }
        solidRect(260.0, 48.0, Colors.WHITE) {
            position(0.0, 522.0)
            alpha = 0.01
            onClick { onMark(field.text) }
        }
        solidRect(240.0, 48.0, Palette.greenFelt) { position(280.0, 522.0) }
        text("Guardar nota", textSize = 16.0, color = Palette.paper) { position(320.0, 536.0); mouseEnabled = false }
        solidRect(240.0, 48.0, Colors.WHITE) {
            position(280.0, 522.0)
            alpha = 0.01
            onClick { onSave(field.text, emptyList()) }
        }
        return 590.0
    } else {
        text("Toca una palabra para resaltarla. Vuelve a tocarla para quitarla.", textSize = 13.0, color = Palette.inkSoft) {
            position(0.0, 418.0)
        }
        scrollBox(0.0, 442.0, 600.0, 160.0, 0.0, onScroll = {}) {
            markedWords(draft, highlights, 0.0, 0.0, 560.0, onToggle) + 8.0
        }
        solidRect(220.0, 48.0, Palette.brassDim) { position(0.0, 620.0) }
        text("Reescribir", textSize = 16.0, color = Palette.paper) { position(52.0, 634.0); mouseEnabled = false }
        solidRect(220.0, 48.0, Colors.WHITE) {
            position(0.0, 620.0)
            alpha = 0.01
            onClick { onDraft(draft); onRewrite() }
        }
        solidRect(240.0, 48.0, Palette.greenFelt) { position(240.0, 620.0) }
        text("Guardar nota", textSize = 16.0, color = Palette.paper) { position(280.0, 634.0); mouseEnabled = false }
        solidRect(240.0, 48.0, Colors.WHITE) {
            position(240.0, 620.0)
            alpha = 0.01
            onClick { onSave(draft, highlights) }
        }
        return 688.0
    }
}

private fun Container.memoryMark(
    assets: AssetBank,
    x: Double,
    y: Double,
    size: Double,
    glyph: String,
    icon: String?,
    active: Boolean,
    help: String,
    showHelp: (Double, Double, String) -> Unit,
    hideHelp: () -> Unit,
    onPress: () -> Unit,
) {
    val plate = container { position(x, y) }
    if (active) {
        plate.solidRect(size + 12.0, size + 18.0, Palette.gold) { position(-6.0, -6.0) }
        plate.solidRect(size, size, Palette.wax)
        plate.solidRect(size - 12.0, size - 12.0, Palette.gold) {
            position(6.0, 6.0)
            mouseEnabled = false
        }
    } else {
        plate.solidRect(size, size, Palette.brass) { alpha = 0.92 }
        plate.solidRect(size - 4.0, size - 4.0, Palette.mahoganyDeep) {
            position(2.0, 2.0)
            alpha = 0.78
            mouseEnabled = false
        }
    }
    val bmp = icon?.let { assets.get(it) }
    if (bmp != null) {
        val pad = if (active) 22.0 else 14.0
        val fitted = min((size - pad) / bmp.width.toDouble(), (size - pad) / bmp.height.toDouble())
        plate.image(bmp) {
            scale(fitted)
            position((size - bmp.width * fitted) / 2.0, (size - bmp.height * fitted) / 2.0)
            mouseEnabled = false
        }
    } else {
        plate.text(glyph, textSize = if (active) 40.0 else 34.0, color = if (active) Palette.wax else Palette.paper) {
            position(if (glyph == "?") size * 0.30 else size * 0.26, size * 0.22)
            mouseEnabled = false
        }
    }
    plate.solidRect(size, size, Colors.WHITE) {
        alpha = 0.01
        onClick {
            hideHelp()
            onPress()
        }
        onOver { showHelp(x, y, help) }
        onOut { hideHelp() }
    }
}

private fun Container.cornerMark(
    x: Double,
    y: Double,
    glyph: String,
    help: String,
    assets: AssetBank? = null,
    icon: String? = null,
    onPress: () -> Unit,
) {
    val plate = container { position(x, y) }
    val hover = container { mouseEnabled = false }
    plate.solidRect(40.0, 40.0, Palette.brass)
    plate.solidRect(36.0, 36.0, Palette.paper) { position(2.0, 2.0); mouseEnabled = false }
    val bmp = if (assets != null && !icon.isNullOrBlank()) assets.get(icon) else null
    if (bmp != null) {
        val box = 28.0
        val fitted = min(box / bmp.width.toDouble().coerceAtLeast(1.0), box / bmp.height.toDouble().coerceAtLeast(1.0))
        plate.image(bmp) {
            scale(fitted)
            position((40.0 - bmp.width * fitted) / 2.0, (40.0 - bmp.height * fitted) / 2.0)
            mouseEnabled = false
        }
    } else {
        plate.text(glyph, textSize = 20.0, color = Palette.ink) { position(11.0, 8.0); mouseEnabled = false }
    }
    plate.solidRect(40.0, 40.0, Colors.WHITE) {
        alpha = 0.01
        onClick { onPress() }
        onOver {
            hover.clearChildren()
            val chipW = help.length * 8.0 + 18.0
            hover.solidRect(chipW, 28.0, Palette.brass) { position(-chipW + 40.0, 44.0); mouseEnabled = false }
            hover.solidRect(chipW - 4.0, 24.0, Palette.paper) { position(-chipW + 42.0, 46.0); mouseEnabled = false }
            hover.text(help, textSize = 13.0, color = Palette.ink) { position(-chipW + 48.0, 50.0); mouseEnabled = false }
        }
        onOut { hover.clearChildren() }
    }
}

private fun openLinked(nav: Navigator, type: ReferenceType, id: String) {
    when (type) {
        ReferenceType.PERSON -> nav.push(Route.Interview(id))
        ReferenceType.DOCUMENT -> nav.push(Route.Document(id))
        ReferenceType.OBJECT -> nav.push(Route.ItemDetail(id))
    }
}

private fun linkHelp(type: ReferenceType): String = when (type) {
    ReferenceType.PERSON -> "Ir al interrogatorio"
    ReferenceType.DOCUMENT -> "Abrir expediente"
    ReferenceType.OBJECT -> "Ver el objeto"
}

private fun Container.memoryHit(x: Double, y: Double, w: Double, h: Double, help: String, onPress: () -> Unit) {
    val host = this
    val hover = container { mouseEnabled = false }
    solidRect(w, h, Colors.WHITE) {
        position(x, y)
        alpha = 0.01
        onClick { onPress() }
        onOver {
            hover.removeFromParent()
            host.addChild(hover)
            hover.clearChildren()
            val chipW = help.length * 8.2 + 18.0
            val chipX = x.coerceAtMost((x + w - chipW).coerceAtLeast(0.0))
            val chipY = y + h + 2.0
            hover.solidRect(chipW, 28.0, Palette.brass) { position(chipX, chipY); mouseEnabled = false }
            hover.solidRect(chipW - 4.0, 24.0, Palette.paper) { position(chipX + 2.0, chipY + 2.0); mouseEnabled = false }
            hover.text(help, textSize = 13.0, color = Palette.ink) { position(chipX + 8.0, chipY + 6.0); mouseEnabled = false }
        }
        onOut { hover.clearChildren() }
    }
}

private fun Container.memoryPortrait(assets: AssetBank, path: String?, x: Double, y: Double, size: Double) {
    solidRect(size, size, Palette.brass) { position(x, y); mouseEnabled = false }
    val bmp = path?.let { assets.get(it) } ?: return
    val inner = size - 6.0
    val fitted = max(inner / bmp.width.toDouble(), inner / bmp.height.toDouble())
    fixedSizeContainer(Size(inner, inner), clip = true) {
        position(x + 3.0, y + 3.0)
        mouseEnabled = false
        image(bmp) {
            scale(fitted)
            position((inner - bmp.width * fitted) / 2.0, (inner - bmp.height * fitted) / 2.0)
            mouseEnabled = false
        }
    }
}

private fun Container.markedWords(
    body: String,
    highlights: List<HighlightRange>,
    x: Double,
    y: Double,
    maxWidth: Double,
    onToggle: ((Int, Int) -> Unit)?,
    textSize: Double = 20.0,
): Double {
    val tokens = Regex("\\S+").findAll(body).toList()
    if (tokens.isEmpty()) return 0.0
    var cx = x
    var cy = y
    val lineH = textSize * 1.45
    for (token in tokens) {
        val word = token.value
        val width = word.length * textSize * 0.56 + 12.0
        if (cx > x && cx + width > x + maxWidth) {
            cx = x
            cy += lineH
        }
        val start = token.range.first
        val end = token.range.last + 1
        val marked = highlights.any { it.start == start && it.endExclusive == end }
        if (marked) {
            solidRect(width, lineH - 4.0, Palette.gold) { position(cx, cy - 2.0); mouseEnabled = false }
        }
        text(word, textSize = textSize, color = if (marked) Palette.mahoganyDeep else Palette.ink) {
            position(cx + 4.0, cy)
            mouseEnabled = false
        }
        if (onToggle != null) {
            solidRect(width, lineH - 4.0, Colors.WHITE) {
                position(cx, cy - 2.0)
                alpha = 0.01
                onClick { onToggle(start, end) }
            }
        }
        cx += width
    }
    return cy + lineH - y
}

private fun Container.memoryHeading(label: String, x: Double, y: Double, width: Double) {
    text(label, textSize = 30.0, color = Palette.ink) { position(x, y); mouseEnabled = false }
    solidRect(width.coerceAtMost(420.0), 5.0, Palette.gold) { position(x, y + 38.0); mouseEnabled = false }
}

private fun Container.memorySubtitle(label: String, x: Double, y: Double, width: Double): Double {
    solidRect(8.0, 28.0, Palette.wax) { position(x, y); mouseEnabled = false }
    text(clipText(label, 32), textSize = 22.0, color = Palette.mahoganyDeep) {
        position(x + 16.0, y)
        mouseEnabled = false
    }
    solidRect(width.coerceAtMost(480.0), 2.0, Palette.brass) { position(x + 16.0, y + 30.0); mouseEnabled = false }
    return 40.0
}

private fun Container.sectionHeading(label: String, x: Double, y: Double, width: Double): Double {
    solidRect(width, 48.0, Palette.paperShade) { position(x, y); mouseEnabled = false }
    solidRect(8.0, 48.0, Palette.wax) { position(x, y); mouseEnabled = false }
    text(clipText(label, 36), textSize = 24.0, color = Palette.mahoganyDeep) {
        position(x + 18.0, y + 10.0)
        mouseEnabled = false
    }
    return 58.0
}

private data class DocWord(val start: Int, val end: Int, val text: String)

private data class DocBlock(val heading: String, val words: List<DocWord>)

private data class DocumentScript(val plain: String, val words: List<DocWord>, val blocks: List<DocBlock>)

private fun documentScript(file: caseclosed.model.CaseFile): DocumentScript {
    val plain = StringBuilder()
    val words = mutableListOf<DocWord>()
    val blocks = mutableListOf<DocBlock>()
    for (section in file.sections) {
        val blockWords = mutableListOf<DocWord>()
        val content = section.content.trim()
        for (match in Regex("\\S+").findAll(content)) {
            if (plain.isNotEmpty() && !plain.endsWith(' ') && !plain.endsWith('\n')) plain.append(' ')
            val start = plain.length
            plain.append(match.value)
            val word = DocWord(start, plain.length, match.value)
            words += word
            blockWords += word
        }
        if (blockWords.isNotEmpty() || section.heading.isNotBlank()) {
            blocks += DocBlock(section.heading, blockWords)
        }
    }
    return DocumentScript(plain.toString(), words, blocks)
}

private fun scriptBodyHeight(script: DocumentScript): Double {
    if (script.words.isEmpty()) return 48.0
    var y = 4.0
    for (block in script.blocks) {
        if (block.heading.isNotBlank()) y += 58.0
        y += phraseRows(block.words, 560.0) * 34.0 + 22.0
    }
    return y
}

private fun phraseRows(words: List<DocWord>, maxWidth: Double): Double {
    if (words.isEmpty()) return 0.0
    var cx = 0.0
    var rows = 1.0
    for (word in words) {
        val width = word.text.length * 22.0 * 0.56 + 10.0
        if (cx > 0.0 && cx + width > maxWidth) {
            cx = 0.0
            rows += 1.0
        }
        cx += width
    }
    return rows
}

private fun Container.flowPhrase(
    words: List<DocWord>,
    firstIndex: Int,
    phrase: IntRange?,
    marks: Set<Int>,
    x: Double,
    y: Double,
    maxWidth: Double,
    onWord: (Int) -> Unit,
    tips: Container,
    hint: (Int) -> String,
): Double {
    if (words.isEmpty()) return 0.0
    val textSize = 22.0
    val lineH = 34.0
    var cx = x
    var cy = y
    words.forEachIndexed { offset, word ->
        val index = firstIndex + offset
        val width = word.text.length * textSize * 0.56 + 10.0
        if (cx > x && cx + width > x + maxWidth) {
            cx = x
            cy += lineH
        }
        val inPhrase = phrase != null && index in phrase
        val emphasized = index in marks
        val fill = when {
            emphasized -> Palette.wax
            inPhrase -> Palette.gold
            else -> null
        }
        if (fill != null) {
            solidRect(width + 4.0, lineH - 6.0, fill) { position(cx - 2.0, cy - 2.0); mouseEnabled = false }
        }
        text(word.text, textSize = textSize, color = if (emphasized) Palette.paper else Palette.ink) {
            position(cx + 2.0, cy)
            mouseEnabled = false
        }
        val wordX = cx
        val wordY = cy
        solidRect(width, lineH - 6.0, Colors.WHITE) {
            position(wordX, wordY - 2.0)
            alpha = 0.01
            onClick { onWord(index) }
            onOver { showPhraseTip(tips, hint(index), wordX, wordY, width, lineH, x, maxWidth) }
            onOut { tips.clearChildren() }
        }
        cx += width
    }
    return cy + lineH - y
}

private fun showPhraseTip(
    tips: Container,
    label: String,
    wordX: Double,
    wordY: Double,
    wordW: Double,
    lineH: Double,
    originX: Double,
    maxWidth: Double,
) {
    tips.clearChildren()
    val chipW = label.length * 8.2 + 18.0
    val limit = originX + maxWidth
    val chipX = (wordX + wordW / 2.0 - chipW / 2.0).coerceIn(originX, (limit - chipW).coerceAtLeast(originX))
    val chipY = if (wordY >= 32.0) wordY - 30.0 else wordY + lineH - 2.0
    tips.solidRect(chipW, 28.0, Palette.brass) { position(chipX, chipY); mouseEnabled = false }
    tips.solidRect(chipW - 4.0, 24.0, Palette.paper) { position(chipX + 2.0, chipY + 2.0); mouseEnabled = false }
    tips.text(label, textSize = 13.0, color = Palette.ink) { position(chipX + 8.0, chipY + 6.0); mouseEnabled = false }
}
