package caseclosed.ui

import caseclosed.model.CaseFile
import caseclosed.model.DocReference
import caseclosed.model.FileSection
import korlibs.image.color.Colors
import korlibs.korge.input.onClick
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.position
import korlibs.korge.view.rotation
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import korlibs.math.geom.degrees

fun buildPhone(root: Container, nav: Navigator) {
    val engine = nav.engine
    val case = engine.activeCase()
    val read = engine.investigation().readMessageIds.toSet()
    root.solidRect(Desk.W, Desk.H, Palette.mahoganyDeep)
    root.chrome("Transcripciones", case.title, nav, true)
    root.scrollBox(24.0, Desk.HEADER + 16.0, 672.0, Desk.H - Desk.HEADER - 32.0, 0.0, onScroll = {}) {
        var y = 8.0
        text("Inspector y forense", textSize = 16.0, color = Palette.brass) { position(8.0, y) }
        y += 36.0
        for (message in case.phoneMessages) {
            val fresh = message.unread && message.id !in read
            solidRect(640.0, 8.0, if (fresh) Palette.wax else Palette.brass) { position(8.0, y) }
            y += 16.0
            text(message.caller, textSize = 22.0, color = Palette.paper) { position(8.0, y) }
            y += 32.0
            text(message.time, textSize = 15.0, color = Palette.brass) { position(8.0, y) }
            y += 28.0
            y += blockText(message.message, 8.0, y, 630.0, 18.0, Palette.paper)
            y += 28.0
        }
        y + 20.0
    }
    engine.markMessagesRead()
}

fun buildFiles(root: Container, nav: Navigator) {
    val engine = nav.engine
    val case = engine.activeCase()
    root.solidRect(Desk.W, Desk.H, Palette.mahoganyDeep)
    root.chrome("Expedientes", case.title, nav, true)
    val files = case.files.sortedBy { it.title.lowercase() }
    root.scrollBox(24.0, Desk.HEADER + 12.0, 672.0, Desk.H - Desk.HEADER - 24.0, 0.0, onScroll = {}) {
        var y = 8.0
        text("Orden alfabético", textSize = 15.0, color = Palette.brass) { position(8.0, y) }
        y += 36.0
        for (file in files) {
            y += fileCard(this, file, y, nav)
        }
        y + 12.0
    }
}

private fun fileCard(parent: Container, file: CaseFile, y: Double, nav: Navigator): Double {
    val h = 168.0
    val card = parent.container { position(0.0, y) }
    card.solidRect(650.0, h, Palette.brass)
    card.solidRect(642.0, h - 8.0, Palette.paper) { position(4.0, 4.0) }
    val thumb = card.container { position(16.0, 20.0) }
    thumb.photo(nav.assets.get(file.thumbnail), 120.0, 120.0, "Archivo", Palette.dossier)
    card.text(file.title, textSize = 22.0, color = Palette.ink) { position(152.0, 18.0) }
    card.text("${file.documentNumber}   ${file.date}", textSize = 14.0, color = Palette.inkSoft) { position(152.0, 52.0) }
    card.blockText(file.shortDescription, 152.0, 78.0, 470.0, 16.0, Palette.ink)
    card.solidRect(650.0, h, Colors.WHITE) {
        alpha = 0.01
        onClick { nav.push(Route.Document(file.id)) }
    }
    return h + 16.0
}

fun buildDocument(root: Container, nav: Navigator, fileId: String) {
    val engine = nav.engine
    val case = engine.activeCase()
    val file = case.files.first { it.id == fileId }
    root.solidRect(Desk.W, Desk.H, Palette.mahoganyDeep)
    root.chrome(clipText(file.title, 24), file.documentNumber, nav, true)
    root.actionButton("Subir", 24.0, Desk.HEADER + 10.0, 120.0, 44.0, Palette.brassDim, Palette.paper) {}
    root.actionButton("Bajar", 156.0, Desk.HEADER + 10.0, 120.0, 44.0, Palette.brassDim, Palette.paper) {}
    val scroller = root.scrollBox(
        x = 24.0,
        y = Desk.HEADER + 64.0,
        w = 672.0,
        h = Desk.H - Desk.HEADER - 80.0,
        initial = engine.investigation().documentScrollPositions[fileId] ?: 0.0,
        onScroll = { engine.setScroll(fileId, it) },
    ) {
        renderDossier(root, this, nav, file)
    }
    // Rebind the up/down hits. The buttons above have empty handlers; add real ones on top.
    root.solidRect(120.0, 44.0, Colors.WHITE) {
        position(24.0, Desk.HEADER + 10.0)
        alpha = 0.01
        onClick { scroller.by(-160.0) }
    }
    root.solidRect(120.0, 44.0, Colors.WHITE) {
        position(156.0, Desk.HEADER + 10.0)
        alpha = 0.01
        onClick { scroller.by(160.0) }
    }
}

private fun renderDossier(host: Container, content: Container, nav: Navigator, file: CaseFile): Double {
    val engine = nav.engine
    val width = 640.0
    content.solidRect(width, 4000.0, Palette.paperShade)
    content.text("EVIDENCIA", textSize = 20.0, color = Palette.wax) {
        position(430.0, 28.0)
        rotation = (-16).degrees
        alpha = 0.55
    }
    var y = 24.0
    content.text(file.title, textSize = 28.0, color = Palette.ink) { position(16.0, y) }
    y += 40.0
    content.text("${file.documentNumber}    ${file.date}", textSize = 15.0, color = Palette.inkSoft) { position(16.0, y) }
    y += 28.0
    y += content.blockText(file.shortDescription, 16.0, y, width - 32.0, 18.0, Palette.ink)
    y += 12.0
    content.text("Toca una frase para extraerla. Las fichas de pista archivan el pasaje.", textSize = 14.0, color = Palette.brassDim) {
        position(16.0, y)
    }
    y += 32.0
    for (section in file.sections) {
        y = renderSection(host, content, nav, file, section, y, width)
    }
    if (file.relatedSuspectIds.isNotEmpty() || file.relatedItemIds.isNotEmpty()) {
        content.text("Índice cruzado", textSize = 18.0, color = Palette.ink) { position(16.0, y) }
        y += 32.0
        for (id in file.relatedSuspectIds) {
            val suspect = engine.activeCase().suspects.firstOrNull { it.id == id } ?: continue
            y = linkRow(content, y, width - 32.0, "Persona  ·  ${suspect.name}") {
                nav.push(Route.Interview(id))
            }
        }
        for (id in file.relatedItemIds) {
            val item = engine.activeCase().items.firstOrNull { it.id == id } ?: continue
            y = linkRow(content, y, width - 32.0, "Prueba  ·  ${item.name}") {
                engine.discoverItem(id)
                nav.push(Route.ItemDetail(id))
            }
        }
    }
    return y + 40.0
}

private fun renderSection(
    host: Container,
    content: Container,
    nav: Navigator,
    file: CaseFile,
    section: FileSection,
    startY: Double,
    width: Double,
): Double {
    val engine = nav.engine
    var y = startY
    content.solidRect(width - 32.0, 3.0, Palette.brass) { position(16.0, y) }
    y += 14.0
    content.text(section.heading, textSize = 22.0, color = Palette.ink) { position(16.0, y) }
    y += 36.0
    val highlights = engine.investigation().highlights
    for (sentence in sentencesOf(section.content)) {
        val saved = highlights.find { it.sourceId == file.id && it.text == sentence }
        val row = content.container { position(16.0, 0.0) }
        val h = row.sentenceBlock(sentence, saved?.note, width - 32.0, y, onTap = {
            host.noteEditor(
                title = "Extracto",
                body = sentence,
                initial = saved?.note ?: "",
                allowPin = true,
            ) { note, pin ->
                val id = engine.saveHighlight(
                    id = saved?.id,
                    sourceType = "document",
                    sourceId = file.id,
                    sectionId = section.id,
                    questionId = null,
                    text = sentence,
                    note = note,
                )
                if (pin) engine.pin("highlight", id, sentence)
                nav.refresh()
            }
        })
        y += h
    }
    section.image?.let { image ->
        val frame = content.container { position(16.0, y) }
        frame.photo(nav.assets.get(image.src), 400.0, 260.0, image.caption, Palette.dossier)
        y += 270.0
        y += content.blockText(image.caption, 16.0, y, width - 32.0, 14.0, Palette.inkSoft)
        y += 12.0
    }
    for (clue in section.highlightableClues.orEmpty()) {
        val saved = highlights.any { it.id == clue.id }
        val label = if (saved) "ANOTADA  ·  ${clipText(clue.text, 42)}" else "PISTA  ·  ${clipText(clue.text, 46)}"
        y = linkRow(content, y, width - 32.0, label, if (saved) Palette.highlight else Palette.gold) {
            val existing = engine.investigation().highlights.find { it.id == clue.id }
            host.noteEditor(
                title = "Pista marcada",
                body = clue.text,
                initial = existing?.note ?: clue.description,
                allowPin = true,
            ) { note, pin ->
                engine.saveHighlight(
                    id = clue.id,
                    sourceType = "document",
                    sourceId = file.id,
                    sectionId = section.id,
                    questionId = null,
                    text = clue.text,
                    note = note,
                )
                if (pin) engine.pin("highlight", clue.id, clue.text)
                nav.refresh()
            }
        }
    }
    for (ref in section.references.orEmpty()) {
        y = linkRow(content, y, width - 32.0, "Abrir  ·  ${ref.label}") {
            openReference(nav, ref)
        }
    }
    return y + 18.0
}

private fun linkRow(
    content: Container,
    y: Double,
    width: Double,
    label: String,
    fill: RGBAFill = Palette.paper,
    onPress: () -> Unit,
): Double {
    content.solidRect(width, 52.0, Palette.brass) { position(16.0, y) }
    content.solidRect(width - 6.0, 46.0, fill) { position(19.0, y + 3.0) }
    content.text(clipText(label, 58), textSize = 16.0, color = Palette.ink) { position(30.0, y + 14.0) }
    content.solidRect(width, 52.0, Colors.WHITE) {
        position(16.0, y)
        alpha = 0.01
        onClick { onPress() }
    }
    return y + 60.0
}

private fun openReference(nav: Navigator, ref: DocReference) {
    when (ref.type) {
        "suspect" -> nav.push(Route.Interview(ref.targetId))
        "item" -> {
            nav.engine.discoverItem(ref.targetId)
            nav.push(Route.ItemDetail(ref.targetId))
        }
        "file" -> nav.push(Route.Document(ref.targetId))
    }
}
