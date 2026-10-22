package caseclosed.ui

import caseclosed.model.Item
import caseclosed.model.Question
import caseclosed.model.Suspect
import korlibs.image.color.Colors
import korlibs.korge.input.onClick
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.position
import korlibs.korge.view.solidRect
import korlibs.korge.view.text

fun buildSuspects(root: Container, nav: Navigator) {
    val case = nav.engine.activeCase()
    root.solidRect(Desk.W, Desk.H, Palette.mahoganyDeep)
    root.chrome("Personas de interés", case.title, nav, true)
    root.scrollBox(24.0, Desk.HEADER + 12.0, 672.0, Desk.H - Desk.HEADER - 24.0, 0.0, onScroll = {}) {
        var y = 8.0
        for (suspect in case.suspects) {
            y += suspectCard(this, suspect, y, nav)
        }
        y
    }
}

private fun suspectCard(parent: Container, suspect: Suspect, y: Double, nav: Navigator): Double {
    val h = 196.0
    val card = parent.container { position(0.0, y) }
    card.solidRect(650.0, h, Palette.brass)
    card.solidRect(642.0, h - 8.0, Palette.paper) { position(4.0, 4.0) }
    val portrait = card.container { position(16.0, 28.0) }
    portrait.photo(nav.assets.get(suspect.portrait), 140.0, 140.0, suspect.name, Palette.dossier)
    card.text(suspect.name, textSize = 22.0, color = Palette.ink) { position(172.0, 20.0) }
    card.text("${suspect.alias}  ·  ${suspect.occupation}", textSize = 14.0, color = Palette.inkSoft) { position(172.0, 52.0) }
    card.blockText(suspect.shortDescription, 172.0, 80.0, 450.0, 16.0, Palette.ink)
    card.solidRect(650.0, h, Colors.WHITE) {
        alpha = 0.01
        onClick { nav.push(Route.Interview(suspect.id)) }
    }
    return h + 16.0
}

fun buildInterview(root: Container, nav: Navigator, suspectId: String) {
    val engine = nav.engine
    val case = engine.activeCase()
    val suspect = case.suspects.first { it.id == suspectId }
    val state = engine.investigation()
    val interview = state.interviews[suspectId]
    root.solidRect(Desk.W, Desk.H, Palette.mahoganyDeep)
    root.chrome(suspect.alias, suspect.name, nav, true)
    root.scrollBox(20.0, Desk.HEADER + 8.0, 680.0, Desk.H - Desk.HEADER - 20.0, 0.0, onScroll = {}) {
        var y = 8.0
        val portrait = container { position(16.0, y) }
        portrait.photo(nav.assets.get(suspect.portrait), 180.0, 180.0, suspect.name, Palette.dossier)
        text(suspect.name, textSize = 26.0, color = Palette.paper) { position(214.0, y + 8.0) }
        text(suspect.occupation, textSize = 16.0, color = Palette.brass) { position(214.0, y + 46.0) }
        y += 196.0
        text("Expediente", textSize = 18.0, color = Palette.brass) { position(8.0, y) }
        y += 28.0
        y += blockText(suspect.shortDescription, 8.0, y, 640.0, 18.0, Palette.paper)
        y += 12.0
        text("Conocido", textSize = 16.0, color = Palette.brass) { position(8.0, y) }
        y += 24.0
        y += blockText(suspect.knownInformation, 8.0, y, 640.0, 17.0, Palette.paper)
        y += 12.0
        text("Coartada", textSize = 16.0, color = Palette.brass) { position(8.0, y) }
        y += 24.0
        y += blockText(suspect.alibi, 8.0, y, 640.0, 17.0, Palette.paper)
        y += 16.0
        val note = state.suspectNotes[suspectId].orEmpty()
        text("Notas del detective", textSize = 16.0, color = Palette.brass) { position(8.0, y) }
        y += 24.0
        y += blockText(if (note.isBlank()) "Sin notas." else note, 8.0, y, 640.0, 17.0, Palette.paper)
        y += 8.0
        val notesY = y
        solidRect(280.0, 48.0, Palette.brass) { position(8.0, notesY) }
        text("Editar notas", textSize = 18.0, color = Palette.ink) { position(24.0, notesY + 13.0) }
        solidRect(280.0, 48.0, Colors.WHITE) {
            position(8.0, notesY)
            alpha = 0.01
            onClick {
                root.noteEditor("Notas sobre ${suspect.name}", "Quedan en el expediente de este sospechoso.", note, allowPin = false) { written, _ ->
                    engine.setSuspectNote(suspectId, written)
                    nav.refresh()
                }
            }
        }
        y += 64.0
        solidRect(280.0, 48.0, Palette.greenFelt) { position(8.0, y) }
        text("Fijar al tablero", textSize = 18.0, color = Palette.paper) { position(24.0, y + 13.0) }
        val pinY = y
        solidRect(280.0, 48.0, Colors.WHITE) {
            position(8.0, pinY)
            alpha = 0.01
            onClick {
                engine.pin("suspect", suspect.id, suspect.shortDescription)
                nav.refresh()
            }
        }
        y += 72.0
        if (interview?.isSaved == true) {
            text("Entrevista registrada", textSize = 14.0, color = Palette.gold) { position(8.0, y) }
            y += 28.0
        }
        text("Respuestas", textSize = 20.0, color = Palette.paper) { position(8.0, y) }
        y += 34.0
        val turns = interview?.conversationOrder.orEmpty().sortedBy { it.timestamp }
        if (turns.isEmpty()) {
            text("Aún no hay preguntas.", textSize = 16.0, color = Palette.paperShade) { position(8.0, y) }
            y += 32.0
        }
        for (turn in turns) {
            val question = suspect.questions.firstOrNull { it.id == turn.questionId } ?: continue
            y = answerBlock(this, root, nav, suspect, question, y)
        }
        text("Preguntar", textSize = 20.0, color = Palette.paper) { position(8.0, y) }
        y += 36.0
        val asked = interview?.askedQuestionIds?.toSet().orEmpty()
        for (question in suspect.questions) {
            if (question.id in asked) continue
            val available = engine.canAskQuestion(
                suspectId,
                question,
            )
            if (!available) continue
            val rowY = y
            solidRect(640.0, 72.0, Palette.paper) {
                position(8.0, rowY)
            }
            text(
                clipText(question.question, 64),
                textSize = 16.0,
                color = Palette.ink,
            ) {
                position(20.0, rowY + 24.0)
            }
            solidRect(640.0, 72.0, Colors.WHITE) {
                position(8.0, rowY)
                alpha = 0.01

                onClick {
                    engine.askQuestion(suspectId, question)
                    nav.refresh()
                }
            }
            y += 84.0
        }
        y += 8.0
        solidRect(320.0, 52.0, Palette.wax) { position(8.0, y) }
        text("Reiniciar entrevista", textSize = 18.0, color = Palette.paper) { position(24.0, y + 14.0) }
        val resetY = y
        solidRect(320.0, 52.0, Colors.WHITE) {
            position(8.0, resetY)
            alpha = 0.01
            onClick {
                root.confirmDialog(
                    "¿Reiniciar esta entrevista?",
                    "Esto borra solo las preguntas y respuestas de ${suspect.name}. Las marcas y el tablero se quedan.",
                    "Reiniciar",
                ) {
                    engine.resetInterview(suspectId)
                    nav.refresh()
                }
            }
        }
        y + 80.0
    }
}

private fun answerBlock(
    content: Container,
    host: Container,
    nav: Navigator,
    suspect: Suspect,
    question: Question,
    startY: Double,
): Double {
    val engine = nav.engine
    var y = startY
    content.text(question.question, textSize = 16.0, color = Palette.brass) { position(8.0, y) }
    y += 28.0
    val highlights = engine.investigation().highlights
    for (sentence in sentencesOf(question.answer)) {
        val saved = highlights.find { it.sourceId == suspect.id && it.text == sentence }
        y += content.sentenceBlock(sentence, saved?.note, 640.0, y) {
            host.noteEditor("Testimonio", sentence, saved?.note ?: "", allowPin = true) { note, pin ->
                val id = engine.saveHighlight(
                    id = saved?.id,
                    sourceType = "interview",
                    sourceId = suspect.id,
                    sectionId = null,
                    questionId = question.id,
                    text = sentence,
                    note = note,
                )
                if (pin) engine.pin("highlight", id, sentence)
                nav.refresh()
            }
        }
    }
    val pinId = "hl_q_${question.id}"
    val rowY = y
    content.solidRect(300.0, 44.0, Palette.gold) { position(8.0, rowY) }
    content.text("Fijar esta respuesta", textSize = 16.0, color = Palette.ink) { position(20.0, rowY + 12.0) }
    content.solidRect(300.0, 44.0, Colors.WHITE) {
        position(8.0, rowY)
        alpha = 0.01
        onClick {
            engine.saveHighlight(
                id = pinId,
                sourceType = "interview",
                sourceId = suspect.id,
                sectionId = null,
                questionId = question.id,
                text = question.answer,
                note = highlights.find { it.id == pinId }?.note ?: "Testimonio",
            )
            engine.pin("highlight", pinId, question.answer)
            nav.refresh()
        }
    }
    return y + 64.0
}

fun buildItems(root: Container, nav: Navigator) {
    val engine = nav.engine
    val case = engine.activeCase()
    val found = case.items.filter { it.id in engine.investigation().discoveredItemIds }
    root.solidRect(Desk.W, Desk.H, Palette.mahoganyDeep)
    root.chrome("Casillero de pruebas", case.title, nav, true)
    root.scrollBox(24.0, Desk.HEADER + 12.0, 672.0, Desk.H - Desk.HEADER - 24.0, 0.0, onScroll = {}) {
        var y = 8.0
        if (found.isEmpty()) {
            y += blockText("El casillero está vacío. Los expedientes, las entrevistas y la escena lo llenan.", 8.0, y, 640.0, 18.0, Palette.paper)
        }
        val hidden = case.items.size - found.size
        if (hidden > 0) {
            text("Faltan $hidden piezas.", textSize = 15.0, color = Palette.brass) { position(8.0, y) }
            y += 32.0
        }
        for (item in found) {
            y += itemCard(this, item, y, nav)
        }
        y
    }
}

private fun itemCard(parent: Container, item: Item, y: Double, nav: Navigator): Double {
    val h = 180.0
    val card = parent.container { position(0.0, y) }
    card.solidRect(650.0, h, Palette.brass)
    card.solidRect(642.0, h - 8.0, Palette.paper) { position(4.0, 4.0) }
    val photo = card.container { position(16.0, 20.0) }
    photo.photo(nav.assets.get(item.image), 140.0, 140.0, item.name, Palette.locker)
    card.text(item.name, textSize = 22.0, color = Palette.ink) { position(172.0, 24.0) }
    card.blockText(item.shortNotes, 172.0, 64.0, 450.0, 16.0, Palette.ink)
    card.solidRect(650.0, h, Colors.WHITE) {
        alpha = 0.01
        onClick { nav.push(Route.ItemDetail(item.id)) }
    }
    return h + 16.0
}

fun buildItemDetail(root: Container, nav: Navigator, itemId: String) {
    val engine = nav.engine
    val case = engine.activeCase()
    val item = case.items.first { it.id == itemId }
    if (itemId !in engine.investigation().discoveredItemIds) {
        root.solidRect(
            Desk.W,
            Desk.H,
            Palette.mahoganyDeep,
        )
        root.chrome(
            "Prueba no disponible",
            case.title,
            nav,
            true,
        )
        root.blockText(
            "Esta prueba aún no se ha descubierto.",
            24.0,
            Desk.HEADER + 24.0,
            640.0,
            18.0,
            Palette.paper,
        )
        return
    }
    val note = engine.investigation().itemNotes[itemId].orEmpty()
    root.solidRect(Desk.W, Desk.H, Palette.mahoganyDeep)
    root.chrome(clipText(item.name, 22), "Casillero forense", nav, true)
    root.scrollBox(20.0, Desk.HEADER + 8.0, 680.0, Desk.H - Desk.HEADER - 20.0, 0.0, onScroll = {}) {
        var y = 8.0
        val frame = container { position(16.0, y) }
        frame.photo(nav.assets.get(item.image), 420.0, 320.0, item.name, Palette.locker)
        y += 340.0
        text(item.name, textSize = 28.0, color = Palette.paper) { position(8.0, y) }
        y += 40.0
        text("Descripción", textSize = 16.0, color = Palette.brass) { position(8.0, y) }
        y += 24.0
        y += blockText(item.description, 8.0, y, 640.0, 18.0, Palette.paper)
        y += 12.0
        text("Hallazgos de laboratorio", textSize = 16.0, color = Palette.brass) { position(8.0, y) }
        y += 24.0
        y += blockText(item.findings, 8.0, y, 640.0, 18.0, Palette.paper)
        y += 12.0
        text("Notas del detective", textSize = 16.0, color = Palette.brass) { position(8.0, y) }
        y += 24.0
        y += blockText(if (note.isBlank()) "Sin notas." else note, 8.0, y, 640.0, 17.0, Palette.paper)
        y += 10.0
        buttonRow(this, y, "Editar notas", Palette.brass, Palette.ink) {
            root.noteEditor("Notas sobre ${item.name}", item.shortNotes, note, allowPin = false) { written, _ ->
                engine.setItemNote(itemId, written)
                nav.refresh()
            }
        }
        y += 64.0
        buttonRow(this, y, "Fijar al tablero", Palette.greenFelt, Palette.paper) {
            engine.pin("item", item.id, item.shortNotes)
            nav.refresh()
        }
        y += 80.0
        if (item.relatedFileIds.isNotEmpty()) {
            text("Documentos relacionados", textSize = 18.0, color = Palette.paper) { position(8.0, y) }
            y += 36.0
            for (fileId in item.relatedFileIds) {
                val file = case.files.firstOrNull { it.id == fileId } ?: continue
                val rowY = y
                solidRect(640.0, 56.0, Palette.paper) { position(8.0, rowY) }
                text(file.title, textSize = 18.0, color = Palette.ink) { position(20.0, rowY + 16.0) }
                solidRect(640.0, 56.0, Colors.WHITE) {
                    position(8.0, rowY)
                    alpha = 0.01
                    onClick { nav.push(Route.Document(fileId)) }
                }
                y += 68.0
            }
        }
        y + 24.0
    }
}

private fun buttonRow(
    content: Container,
    y: Double,
    label: String,
    fill: RGBAFill,
    ink: RGBAFill,
    onPress: () -> Unit,
) {
    content.solidRect(300.0, 52.0, fill) { position(8.0, y) }
    content.text(label, textSize = 18.0, color = ink) { position(24.0, y + 14.0) }
    content.solidRect(300.0, 52.0, Colors.WHITE) {
        position(8.0, y)
        alpha = 0.01
        onClick { onPress() }
    }
}
