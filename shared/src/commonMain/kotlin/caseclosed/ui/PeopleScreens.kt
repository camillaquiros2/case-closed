package caseclosed.ui

import caseclosed.data.ButtonIcons
import caseclosed.model.CaseData
import caseclosed.model.Item
import caseclosed.model.Question
import caseclosed.model.Suspect
import korlibs.image.bitmap.Bitmap
import korlibs.image.color.Colors
import korlibs.korge.input.onClick
import korlibs.korge.view.Container
import korlibs.korge.view.alpha
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.korge.view.fixedSizeContainer
import korlibs.korge.view.image
import korlibs.korge.view.position
import korlibs.korge.view.scale
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import korlibs.math.geom.Size
import kotlin.math.max
import kotlin.time.DurationUnit

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
    var note = engine.investigation().suspectNotes[suspectId].orEmpty()
    val dialogueTop = Desk.H - 320.0

    val railW = 104.0
    root.solidRect(Desk.W, Desk.H, Palette.mahoganyDeep)
    root.portraitPlate(
        nav.assets.get(suspect.portrait),
        0.0,
        Desk.HEADER,
        Desk.W - railW,
        dialogueTop - Desk.HEADER,
    )
    root.solidRect(railW, dialogueTop - Desk.HEADER, Palette.mahogany) {
        position(Desk.W - railW, Desk.HEADER)
    }
    root.solidRect(3.0, dialogueTop - Desk.HEADER, Palette.brass) {
        position(Desk.W - railW, Desk.HEADER)
    }
    root.chrome(suspect.alias, suspect.name, nav, true)

    val mentions = interviewMentions(case)
    val dialogue = root.container { position(24.0, dialogueTop) }
    dialogue.solidRect(672.0, 296.0, Palette.brass)
    dialogue.solidRect(660.0, 284.0, Palette.paper) { position(6.0, 6.0) }
    dialogue.text(suspect.name, textSize = 22.0, color = Palette.wax) { position(22.0, 18.0) }
    val questionLine = dialogue.text("", textSize = 15.0, color = Palette.brassDim) { position(22.0, 52.0) }
    val typed = dialogue.container { position(22.0, 84.0) }
    var full = ""
    var shown = 0
    var running = false
    var speaking: Question? = null

    fun paint() {
        typed.clearChildren()
        if (shown < full.length) {
            typed.blockText(full.take(shown), 0.0, 0.0, 620.0, 20.0, Palette.ink)
        } else {
            typed.drawLinkedAnswer(full, interviewLinkSpans(full, mentions), 620.0, 20.0) { spot ->
                if (spot.itemId != null) {
                    nav.push(Route.SceneFind(spot.sceneId, spot.subSceneId, spot.itemId))
                } else {
                    nav.push(Route.SceneCloseup(spot.sceneId, spot.subSceneId))
                }
            }
        }
    }

    val cue = dialogue.text("seguir ▸", textSize = 16.0, color = Palette.brassDim) {
        position(548.0, 250.0)
        visible = false
    }
    var boardEnabled = false
    var boardIcon: Container? = null
    var boardCaption: korlibs.korge.view.Text? = null
    var finishHit: korlibs.korge.view.View? = null

    fun refreshPin() {
        val question = speaking
        val filed = question != null && engine.investigation().filedTestimonies.any {
            it.suspectId == suspectId && it.questionId == question.id
        }
        boardEnabled = !running && question != null && !filed
        boardIcon?.alpha = if (!running && question != null) 1.0 else 0.38
        boardCaption?.text = if (filed) "Guardada" else "Evidencia"
        cue.visible = running
        finishHit?.mouseEnabled = running
    }

    fun present(text: String, question: Question?, instant: Boolean) {
        full = text
        speaking = question
        questionLine.text = if (question == null) "" else clipText(question.question, 78)
        shown = if (instant) full.length else 0
        running = shown < full.length
        paint()
        refreshPin()
    }

    var carry = 0.0
    finishHit = dialogue.solidRect(660.0, 284.0, Colors.WHITE) {
        position(6.0, 6.0)
        alpha = 0.01
        onClick {
            if (running) {
                shown = full.length
                running = false
                carry = 0.0
                paint()
                refreshPin()
            }
        }
    }

    dialogue.addUpdater { dt ->
        if (!running) return@addUpdater
        carry += dt.toDouble(DurationUnit.SECONDS) * 42.0
        val step = carry.toInt()
        if (step <= 0) return@addUpdater
        carry -= step
        shown = (shown + step).coerceAtMost(full.length)
        if (shown >= full.length) {
            running = false
            carry = 0.0
        }
        paint()
        refreshPin()
    }

    val lastId = engine.investigation().interviews[suspectId]
        ?.conversationOrder
        ?.maxByOrNull { it.timestamp }
        ?.questionId
    val last = suspect.questions.firstOrNull { it.id == lastId }
    if (last != null) {
        present(last.answer, last, instant = true)
    } else {
        present("Puede preguntar.", null, instant = false)
    }

    val railX = Desk.W - railW + 20.0
    fun slot(index: Int) = Desk.HEADER + 36.0 + index * 118.0
    root.interviewIcon(railX, slot(0), nav.assets, ButtonIcons.PREGUNTA) {
        root.questionDialog(
            nav,
            suspect,
            onAsk = { question ->
                engine.askQuestion(suspectId, question)
                present(question.answer, question, instant = false)
            },
            onReplay = { question -> present(question.answer, question, instant = false) },
        )
    }
    root.text("Preguntas", textSize = 13.0, color = Palette.paper) { position(railX - 2.0, slot(0) + 68.0) }
    root.interviewIcon(railX, slot(1), nav.assets, ButtonIcons.NOTAS) {
        root.noteEditor(
            "Notas sobre ${suspect.name}",
            "Quedan en el expediente de este sospechoso.",
            note,
            allowPin = false,
            assets = nav.assets,
        ) { written, _ ->
            engine.setSuspectNote(suspectId, written)
            note = written
        }
    }
    root.text("Notas", textSize = 13.0, color = Palette.paper) { position(railX + 12.0, slot(1) + 68.0) }
    boardIcon = root.interviewIcon(railX, slot(2), nav.assets, ButtonIcons.ACUSAR) {
        if (!boardEnabled) return@interviewIcon
        val question = speaking ?: return@interviewIcon
        if (engine.fileTestimony(suspectId, question.id)) refreshPin()
    }
    boardCaption = root.text("Evidencia", textSize = 13.0, color = Palette.paper) {
        position(railX - 4.0, slot(2) + 68.0)
    }
    refreshPin()
}

private fun Container.questionDialog(
    nav: Navigator,
    suspect: Suspect,
    onAsk: (Question) -> Unit,
    onReplay: (Question) -> Unit,
) {
    val engine = nav.engine
    val asked = engine.investigation().interviews[suspect.id]?.askedQuestionIds?.toSet().orEmpty()
    val pending = suspect.questions.filter { question ->
        question.id !in asked && engine.canAskQuestion(suspect.id, question)
    }
    val answered = suspect.questions.filter { it.id in asked }
    var list = if (pending.isEmpty()) 64.0 else 0.0
    list += pending.sumOf { questionChoiceHeight(it) }
    if (answered.isNotEmpty()) list += 40.0 + answered.sumOf { questionChoiceHeight(it) }
    val buttonY = 56.0 + list + 12.0
    val panelH = (buttonY + 132.0).coerceIn(520.0, 1080.0)
    overlay(660.0, panelH, dismissOnScrim = true) { dismiss ->
        text("Preguntas", textSize = 26.0, color = Palette.ink) { position(0.0, 0.0) }
        var y = 52.0
        if (pending.isEmpty()) {
            y += blockText(
                "Ahora no tiene nada más que decir.",
                0.0, y, 580.0, 18.0, Palette.inkSoft,
            )
            y += 8.0
        }
        for (question in pending) {
            y = questionChoice(question, y) {
                dismiss()
                onAsk(question)
            }
        }
        if (answered.isNotEmpty()) {
            text("Ya respondidas", textSize = 16.0, color = Palette.brassDim) { position(0.0, y) }
            y += 32.0
            for (question in answered) {
                y = questionChoice(question, y) {
                    dismiss()
                    onReplay(question)
                }
            }
        }
        actionButton(
            "Reiniciar entrevista",
            0.0,
            buttonY,
            320.0,
            56.0,
            Palette.wax,
            Palette.paper,
            nav.assets,
            ButtonIcons.REINICIAR,
        ) {
            this@questionDialog.confirmDialog(
                "¿Reiniciar esta entrevista?",
                "Esto borra solo las preguntas y respuestas de ${suspect.name}. Las marcas y el tablero se quedan.",
                "Reiniciar",
                nav.assets,
                ButtonIcons.REINICIAR,
            ) {
                nav.engine.resetInterview(suspect.id)
                dismiss()
                nav.refresh()
            }
        }
    }
}

private fun questionChoiceHeight(question: Question): Double =
    32.0 + wrapLines(question.question, 560.0, 18.0).size * 24.0

private fun Container.questionChoice(question: Question, y: Double, onPress: () -> Unit): Double {
    val lines = wrapLines(question.question, 560.0, 18.0)
    val height = 20.0 + lines.size * 24.0
    solidRect(600.0, height, Palette.paperShade) { position(0.0, y) }
    lines.forEachIndexed { index, line ->
        text(line, textSize = 18.0, color = Palette.ink) { position(16.0, y + 12.0 + index * 24.0) }
    }
    solidRect(600.0, height, Colors.WHITE) {
        position(0.0, y)
        alpha = 0.01
        onClick { onPress() }
    }
    return y + height + 12.0
}

private fun Container.interviewIcon(
    x: Double,
    y: Double,
    assets: AssetBank,
    icon: String,
    onPress: () -> Unit,
): Container {
    val size = 64.0
    val box = container { position(x, y) }
    box.solidRect(size, size, Palette.brass)
    box.solidRect(size - 6.0, size - 6.0, Palette.wax) { position(3.0, 3.0) }
    val bmp = assets.get(icon)
    if (bmp != null) {
        val fitted = minOf(44.0 / bmp.width, 44.0 / bmp.height)
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
    return box
}

private fun Container.portraitPlate(bmp: Bitmap?, x: Double, y: Double, w: Double, h: Double) {
    fixedSizeContainer(Size(w, h), clip = true) {
        position(x, y)
        if (bmp == null) {
            solidRect(w, h, Palette.dossier)
            return@fixedSizeContainer
        }
        val fitted = max(w / bmp.width, h / bmp.height)
        image(bmp) {
            scale(fitted)
            val dw = bmp.width * fitted
            val dh = bmp.height * fitted
            position((w - dw) / 2.0, (h - dh) * 0.12)
            mouseEnabled = false
        }
    }
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
        buttonRow(this, y, "Editar notas", nav.assets, ButtonIcons.NOTAS) {
            root.noteEditor("Notas sobre ${item.name}", item.shortNotes, note, allowPin = false, assets = nav.assets) { written, _ ->
                engine.setItemNote(itemId, written)
                nav.refresh()
            }
        }
        y += 64.0
        buttonRow(this, y, "Fijar al tablero", nav.assets, ButtonIcons.FIJAR) {
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

internal data class InterviewSpot(val sceneId: String, val subSceneId: String, val itemId: String?)

internal data class InterviewLink(val start: Int, val endExclusive: Int, val target: InterviewSpot)

private val mentionSkip = setOf(
    "de", "del", "la", "el", "los", "las", "con", "por", "una", "uno", "para", "sobre", "sus", "su",
    "the", "and", "for", "with", "from", "that", "this",
)

internal fun interviewMentions(case: CaseData): List<Pair<String, InterviewSpot>> {
    val places = case.scenes.flatMap { it.subscenes }.map { it.name.lowercase() }.toSet()
    val mentions = mutableListOf<Pair<String, InterviewSpot>>()
    for (scene in case.scenes) {
        for (sub in scene.subscenes) {
            val itemIds = sub.items.map { it.itemId }
            if (sub.name.isNotBlank()) {
                mentions += sub.name to InterviewSpot(scene.id, sub.id, itemIds.singleOrNull())
            }
            for (id in itemIds) {
                val item = case.items.firstOrNull { it.id == id } ?: continue
                for (phrase in mentionPhrases(item.name, places)) {
                    mentions += phrase to InterviewSpot(scene.id, sub.id, id)
                }
            }
        }
    }
    return mentions
        .distinctBy { it.first.lowercase() }
        .sortedByDescending { it.first.length }
}

internal fun interviewLinkSpans(text: String, mentions: List<Pair<String, InterviewSpot>>): List<InterviewLink> {
    val lower = text.lowercase()
    val taken = BooleanArray(text.length)
    val found = mutableListOf<InterviewLink>()
    for ((phrase, target) in mentions) {
        val needle = phrase.lowercase()
        if (needle.length < 3) continue
        var from = 0
        while (from < lower.length) {
            val at = lower.indexOf(needle, from)
            if (at < 0) break
            val end = at + needle.length
            val leftOk = at == 0 || !lower[at - 1].isLetter()
            val rightOk = end >= lower.length || !lower[end].isLetter()
            val free = (at until end).all { !taken[it] }
            if (leftOk && rightOk && free) {
                for (index in at until end) taken[index] = true
                found += InterviewLink(at, end, target)
            }
            from = at + needle.length.coerceAtLeast(1)
        }
    }
    return found.sortedBy { it.start }
}

private fun mentionPhrases(name: String, places: Set<String>): List<String> {
    val phrases = mutableListOf(name.trim())
    Regex("[\\p{L}\\p{N}]+\\s*\\([^)]+\\)").find(name)?.value?.trim()?.let { phrases += it }
    Regex("\\(([^)]+)\\)").find(name)?.groupValues?.get(1)?.trim()?.let { inner ->
        if (inner.length >= 4) phrases += inner
    }
    val base = name.replace(Regex("\\s*\\([^)]*\\)"), " ").replace(Regex("\\s+"), " ").trim()
    if (base.isNotEmpty()) phrases += base
    phrases += base.split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { word ->
            val key = word.lowercase()
            word.length >= 4 && key !in mentionSkip && key !in places
        }
    return phrases.filter { it.length >= 3 }.distinctBy { it.lowercase() }
}

private fun Container.drawLinkedAnswer(
    text: String,
    links: List<InterviewLink>,
    width: Double,
    textSize: Double,
    onOpen: (InterviewSpot) -> Unit,
) {
    val lineH = textSize * 1.35
    val gap = textSize * 0.28
    var cx = 0.0
    var cy = 0.0
    for (token in Regex("\\S+").findAll(text)) {
        val word = token.value
        val wordW = word.length * textSize * 0.52
        if (cx > 0.0 && cx + wordW > width) {
            cx = 0.0
            cy += lineH
        }
        val link = links.firstOrNull { token.range.first < it.endExclusive && token.range.last >= it.start }
        if (link != null) {
            solidRect(wordW + 6.0, lineH - 2.0, Palette.gold) {
                position(cx - 2.0, cy - 1.0)
                alpha = 0.9
                mouseEnabled = false
            }
        }
        text(word, textSize = textSize, color = if (link != null) Palette.mahoganyDeep else Palette.ink) {
            position(cx, cy)
            mouseEnabled = false
        }
        if (link != null) {
            val spot = link.target
            solidRect(wordW + gap, lineH, Colors.WHITE) {
                position(cx, cy)
                alpha = 0.01
                onClick { onOpen(spot) }
            }
        }
        cx += wordW + gap
    }
}

private fun buttonRow(
    content: Container,
    y: Double,
    label: String,
    assets: AssetBank,
    icon: String,
    onPress: () -> Unit,
) {
    content.actionButton(label, 8.0, y, 320.0, 52.0, Palette.wax, Palette.paper, assets, icon, onPress)
}
