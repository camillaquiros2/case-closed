package caseclosed.ui

import caseclosed.data.ButtonIcons
import caseclosed.data.CaseCatalog
import caseclosed.model.CaseData
import caseclosed.model.CaseInvestigationState
import caseclosed.model.ReferenceType
import caseclosed.model.Verdict
import korlibs.image.color.Colors
import korlibs.korge.input.onClick
import korlibs.korge.view.Container
import korlibs.korge.view.View
import korlibs.korge.view.addUpdater
import korlibs.korge.view.alpha
import korlibs.korge.view.anchor
import korlibs.korge.view.container
import korlibs.korge.view.fixedSizeContainer
import korlibs.korge.view.image
import korlibs.korge.view.position
import korlibs.korge.view.rotation
import korlibs.korge.view.scale
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import korlibs.math.geom.Size
import korlibs.math.geom.degrees
import kotlin.math.max
import kotlin.time.DurationUnit

fun buildConclusion(root: Container, nav: Navigator) {
    val engine = nav.engine
    val case = engine.activeCase()
    root.solidRect(Desk.W, Desk.H, Palette.mahoganyDeep)
    root.chrome("Acusación", case.title, nav, true)

    val accused = arrayOfNulls<String>(1)
    val picked = linkedSetOf<String>()
    val faceMarks = mutableMapOf<String, View>()
    var filter = "todos"

    val faceTop = Desk.HEADER + 12.0
    val gap = 14.0
    val cell = (688.0 - gap * 3) / 4.0
    val portrait = 112.0
    case.suspects.forEachIndexed { index, suspect ->
        val x = 16.0 + index * (cell + gap)
        val frame = root.solidRect(portrait + 8.0, portrait + 8.0, Palette.gold) {
            position(x + (cell - portrait - 8.0) / 2.0, faceTop)
            alpha = 0.0
            mouseEnabled = false
        }
        faceMarks[suspect.id] = frame
        root.chargePortrait(nav.assets, suspect.portrait, x + (cell - portrait) / 2.0, faceTop + 4.0, portrait)
        val name = clipText(suspect.name, 16)
        val nameX = x + (cell - name.length * 7.2) / 2.0
        root.text(name, textSize = 14.0, color = Palette.paper) {
            position(nameX.coerceAtLeast(x), faceTop + portrait + 14.0)
            mouseEnabled = false
        }
        root.solidRect(cell, portrait + 42.0, Colors.WHITE) {
            position(x, faceTop)
            alpha = 0.01
            onClick {
                accused[0] = suspect.id
                for ((id, box) in faceMarks) box.alpha = if (id == suspect.id) 1.0 else 0.0
            }
        }
    }

    val filterY = faceTop + portrait + 48.0
    val filters = listOf(
        "todos" to ButtonIcons.TAB_TODOS,
        "objetos" to ButtonIcons.TAB_OBJETOS,
        "personas" to ButtonIcons.TAB_SOSPECHOSOS,
        "expedientes" to ButtonIcons.TAB_EXPEDIENTES,
    )
    val filterMarks = mutableMapOf<String, View>()
    lateinit var paintList: () -> Unit
    filters.forEachIndexed { index, (id, icon) ->
        val button = root.container { position(16.0 + index * 64.0, filterY) }
        button.solidRect(52.0, 52.0, Palette.brass)
        val glow = button.solidRect(52.0, 52.0, Palette.gold) {
            alpha = if (id == filter) 1.0 else 0.0
            mouseEnabled = false
        }
        filterMarks[id] = glow
        button.solidRect(46.0, 46.0, Palette.mahoganyDeep) {
            position(3.0, 3.0)
            mouseEnabled = false
        }
        val bmp = nav.assets.get(icon)
        if (bmp != null) {
            val fitted = kotlin.math.min(36.0 / bmp.width.toDouble(), 36.0 / bmp.height.toDouble())
            button.image(bmp) {
                scale(fitted)
                position((52.0 - bmp.width * fitted) / 2.0, (52.0 - bmp.height * fitted) / 2.0)
                mouseEnabled = false
            }
        }
        button.solidRect(52.0, 52.0, Colors.WHITE) {
            alpha = 0.01
            onClick {
                filter = id
                for ((key, box) in filterMarks) box.alpha = if (key == filter) 1.0 else 0.0
                paintList()
            }
        }
    }

    val listTop = filterY + 64.0
    val listHost = root.container()
    paintList = {
        listHost.clearChildren()
        val cards = chargeCards(case, engine.investigation(), filter)
        listHost.scrollBox(16.0, listTop, 688.0, Desk.H - listTop - 128.0, 0.0, onScroll = {}) {
            var y = 4.0
            if (filter == "todos" || filter == "personas") {
                actionButton(
                    "Añadir respuesta",
                    8.0,
                    y,
                    360.0,
                    52.0,
                    Palette.wax,
                    Palette.paper,
                    nav.assets,
                    ButtonIcons.PREGUNTA,
                ) {
                    root.answerEvidenceDialog(nav) { paintList() }
                }
                y += 64.0
            }
            if (cards.isEmpty()) {
                y += blockText(
                    "Sin registro todavía. Vincula un expediente, escribe una nota o guarda una respuesta.",
                    8.0,
                    y,
                    640.0,
                    18.0,
                    Palette.paper,
                )
            }
            for (card in cards) {
                val textX = 112.0
                val titleW = 470.0
                val bodyW = 520.0
                val titleLines = wrapLines(card.title, titleW, 20.0)
                val subLines = wrapLines(card.subtitle, titleW, 16.0)
                val bodyLines = wrapLines(card.excerpt, bodyW, 18.0)
                val rowH = (
                    20.0 + titleLines.size * 26.0 + subLines.size * 22.0 + 6.0 + bodyLines.size * 24.0 + 12.0
                    ).coerceAtLeast(108.0)
                val row = y
                val frame = solidRect(660.0, rowH, if (card.id in picked) Palette.gold else Palette.brass) {
                    position(8.0, row)
                }
                solidRect(652.0, rowH - 8.0, Palette.paper) {
                    position(12.0, row + 4.0)
                    mouseEnabled = false
                }
                chargePortrait(nav.assets, card.image, 22.0, row + 16.0, 72.0)
                var lineY = row + 14.0
                solidRect(6.0, 22.0, Palette.wax) {
                    position(textX, lineY + 2.0)
                    mouseEnabled = false
                }
                for (line in titleLines) {
                    text(line, textSize = 20.0, color = Palette.mahoganyDeep) {
                        position(textX + 14.0, lineY)
                        mouseEnabled = false
                    }
                    lineY += 26.0
                }
                for (line in subLines) {
                    text(line, textSize = 16.0, color = Palette.inkSoft) {
                        position(textX + 14.0, lineY)
                        mouseEnabled = false
                    }
                    lineY += 22.0
                }
                lineY += 6.0
                for (line in bodyLines) {
                    text(line, textSize = 18.0, color = Palette.ink) {
                        position(textX, lineY)
                        mouseEnabled = false
                    }
                    lineY += 24.0
                }
                solidRect(28.0, 28.0, Palette.paperShade) {
                    position(608.0, row + 16.0)
                    mouseEnabled = false
                }
                val mark = solidRect(18.0, 18.0, Palette.wax) {
                    position(613.0, row + 21.0)
                    alpha = if (card.id in picked) 1.0 else 0.0
                    mouseEnabled = false
                }
                val cardId = card.id
                solidRect(660.0, rowH, Colors.WHITE) {
                    position(8.0, row)
                    alpha = 0.01
                    onClick {
                        val on = cardId !in picked
                        if (on) picked.add(cardId) else picked.remove(cardId)
                        mark.alpha = if (on) 1.0 else 0.0
                        frame.color = if (on) Palette.gold else Palette.brass
                    }
                }
                y += rowH + 12.0
            }
            y + 12.0
        }
    }
    paintList()

    root.actionButton("Presentar acusación", 24.0, Desk.H - 110.0, 672.0, 72.0, Palette.wax, Palette.paper, nav.assets, ButtonIcons.ACUSAR) {
        val suspectId = accused[0]
        if (suspectId == null) {
            root.confirmDialog(
                "Sin nombre en el expediente",
                "Elige a la persona que acusas antes de enviar esto arriba.",
                "Volver",
                nav.assets,
                ButtonIcons.VOLVER,
            ) {}
            return@actionButton
        }
        val name = case.suspects.first { it.id == suspectId }.name
        root.confirmDialog(
            "¿Estás seguro?",
            "¿Archivar esta acusación contra $name? Un nombre equivocado queda en tu expediente. Si el jefe la rechaza, las notas siguen donde estaban.",
            "Presentar",
            nav.assets,
            ButtonIcons.ACUSAR,
        ) {
            val verdict = engine.evaluate(suspectId, picked)
            presentVerdict(root, nav, verdict)
        }
    }
}

private data class ChargeCard(
    val id: String,
    val image: String?,
    val title: String,
    val subtitle: String,
    val excerpt: String,
)

private fun chargeCards(case: CaseData, state: CaseInvestigationState, filter: String): List<ChargeCard> {
    val cards = mutableListOf<ChargeCard>()
    if (filter == "todos" || filter == "personas") {
        for (filed in state.filedTestimonies) {
            val suspect = case.suspects.firstOrNull { it.id == filed.suspectId } ?: continue
            val question = suspect.questions.firstOrNull { it.id == filed.questionId } ?: continue
            cards += ChargeCard(
                filed.id,
                suspect.portrait,
                suspect.name,
                question.question,
                question.answer,
            )
        }
    }
    for ((objectId, memory) in state.objectMemories) {
        val host = case.items.firstOrNull { it.id == objectId }
        val hostName = host?.name ?: "Objeto"
        for (document in memory.documents) {
            val file = case.files.firstOrNull { it.id == document.documentId }
            when (filter) {
                "objetos" -> cards += ChargeCard(
                    document.id,
                    host?.image,
                    hostName,
                    file?.title ?: "Expediente",
                    document.fragment,
                )
                "todos", "expedientes" -> cards += ChargeCard(
                    document.id,
                    file?.thumbnail ?: host?.image,
                    file?.title ?: "Expediente",
                    "Vinculado a $hostName",
                    document.fragment,
                )
            }
        }
        for (note in memory.notes) {
            if (note.referenceType == ReferenceType.PERSON) continue
            val kind = when (note.referenceType) {
                ReferenceType.OBJECT -> "objetos"
                ReferenceType.DOCUMENT -> "expedientes"
                ReferenceType.PERSON -> continue
            }
            if (filter != "todos" && filter != kind) continue
            val image = when (note.referenceType) {
                ReferenceType.OBJECT -> case.items.firstOrNull { it.id == note.referenceId }?.image
                ReferenceType.DOCUMENT -> case.files.firstOrNull { it.id == note.referenceId }?.thumbnail
                ReferenceType.PERSON -> null
            }
            val title = when (note.referenceType) {
                ReferenceType.OBJECT -> case.items.firstOrNull { it.id == note.referenceId }?.name ?: "Objeto"
                ReferenceType.DOCUMENT -> case.files.firstOrNull { it.id == note.referenceId }?.title ?: "Expediente"
                ReferenceType.PERSON -> continue
            }
            val about = if (note.referenceType == ReferenceType.OBJECT && note.referenceId == objectId) {
                "Nota mental"
            } else {
                "Nota sobre $hostName"
            }
            cards += ChargeCard(note.id, image ?: host?.image, title, about, note.text)
        }
    }
    return cards
}

private fun Container.answerEvidenceDialog(nav: Navigator, onFiled: () -> Unit) {
    val engine = nav.engine
    val case = engine.activeCase()
    overlay(660.0, 980.0, dismissOnScrim = true) { dismiss ->
        val body = container()
        var suspectId: String? = null
        var questionId: String? = null
        fun paint() {
            body.clearChildren()
            val asked = engine.investigation().interviews
            val suspect = case.suspects.firstOrNull { it.id == suspectId }
            val question = suspect?.questions?.firstOrNull { it.id == questionId }
            when {
                suspect == null -> {
                    body.text("Elige a la persona", textSize = 26.0, color = Palette.ink)
                    val people = case.suspects.filter { asked[it.id]?.askedQuestionIds?.isNotEmpty() == true }
                    if (people.isEmpty()) {
                        body.blockText(
                            "Nadie ha respondido todavía. Interroga primero y vuelve a guardar la respuesta.",
                            0.0,
                            56.0,
                            590.0,
                            18.0,
                            Palette.ink,
                        )
                    } else {
                        body.scrollBox(0.0, 52.0, 620.0, 760.0, 0.0, onScroll = {}) {
                            var y = 0.0
                            for (person in people) {
                                val row = y
                                solidRect(600.0, 84.0, Palette.paperShade) { position(0.0, row) }
                                chargePortrait(nav.assets, person.portrait, 10.0, row + 10.0, 64.0)
                                text(person.name, textSize = 20.0, color = Palette.ink) {
                                    position(88.0, row + 28.0)
                                    mouseEnabled = false
                                }
                                solidRect(600.0, 84.0, Colors.WHITE) {
                                    position(0.0, row)
                                    alpha = 0.01
                                    onClick {
                                        suspectId = person.id
                                        paint()
                                    }
                                }
                                y += 96.0
                            }
                            y
                        }
                    }
                    body.actionButton("Cerrar", 0.0, 860.0, 240.0, 56.0, Palette.brassDim, Palette.paper, nav.assets, ButtonIcons.CERRAR) {
                        dismiss()
                    }
                }
                question == null -> {
                    body.text(clipText(suspect.name, 32), textSize = 26.0, color = Palette.ink)
                    val answered = suspect.questions.filter { it.id in asked[suspect.id]?.askedQuestionIds.orEmpty() }
                    val filed = engine.investigation().filedTestimonies
                        .filter { it.suspectId == suspect.id }
                        .map { it.questionId }
                        .toSet()
                    body.scrollBox(0.0, 52.0, 620.0, 760.0, 0.0, onScroll = {}) {
                        var y = 0.0
                        for (item in answered) {
                            val lines = wrapLines(item.question, 540.0, 18.0)
                            val height = 44.0 + lines.size * 24.0
                            val row = y
                            solidRect(600.0, height, Palette.paperShade) { position(0.0, row) }
                            lines.forEachIndexed { index, line ->
                                text(line, textSize = 18.0, color = Palette.ink) {
                                    position(14.0, row + 10.0 + index * 24.0)
                                    mouseEnabled = false
                                }
                            }
                            text(if (item.id in filed) "En la acusación" else "Ver respuesta", textSize = 14.0, color = Palette.wax) {
                                position(14.0, row + 16.0 + lines.size * 24.0)
                                mouseEnabled = false
                            }
                            solidRect(600.0, height, Colors.WHITE) {
                                position(0.0, row)
                                alpha = 0.01
                                onClick {
                                    questionId = item.id
                                    paint()
                                }
                            }
                            y += height + 12.0
                        }
                        y
                    }
                    body.actionButton("Volver", 0.0, 860.0, 240.0, 56.0, Palette.brassDim, Palette.paper, nav.assets, ButtonIcons.VOLVER) {
                        suspectId = null
                        paint()
                    }
                }
                else -> {
                    val questionH = body.blockText(question.question, 0.0, 0.0, 590.0, 20.0, Palette.wax)
                    body.scrollBox(0.0, questionH + 16.0, 620.0, (680.0 - questionH).coerceAtLeast(240.0), 0.0, onScroll = {}) {
                        blockText(question.answer, 0.0, 0.0, 590.0, 20.0, Palette.ink) + 12.0
                    }
                    val already = engine.investigation().filedTestimonies.any {
                        it.suspectId == suspect.id && it.questionId == question.id
                    }
                    body.actionButton(
                        if (already) "Ya en la acusación" else "Añadir a evidencia",
                        0.0,
                        720.0,
                        420.0,
                        64.0,
                        if (already) Palette.brassDim else Palette.wax,
                        Palette.paper,
                        nav.assets,
                        ButtonIcons.ACUSAR,
                    ) {
                        if (!already && engine.fileTestimony(suspect.id, question.id)) {
                            onFiled()
                            paint()
                        }
                    }
                    body.actionButton("Volver", 0.0, 800.0, 240.0, 56.0, Palette.brassDim, Palette.paper, nav.assets, ButtonIcons.VOLVER) {
                        questionId = null
                        paint()
                    }
                }
            }
        }
        paint()
    }
}

private fun Container.chargePortrait(assets: AssetBank, path: String?, x: Double, y: Double, size: Double) {
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

private const val CLOSING_IMAGE = "images/cases/case_001/ending/closing.jpg"

private fun presentVerdict(root: Container, nav: Navigator, verdict: Verdict) {
    val caseId = nav.engine.activeCase().id
    val solved = verdict is Verdict.Solved
    val nextId = if (solved) nav.engine.applySuccess() else null
    if (solved && nextId != null) {
        val title = CaseCatalog.require(nextId).data.title
        nav.banner = "$title ya está en el escritorio."
    } else if (solved) {
        nav.banner = "El armario está despejado."
    }
    val message = when (verdict) {
        is Verdict.Solved -> verdict.explanation
        is Verdict.Rejected -> verdict.critique
    }
    if (solved && caseId == "case_001") {
        root.closingTriptych(nav, message) { nav.go(Route.Desk) }
        return
    }
    val heading = if (solved) "Caso cerrado" else "Expediente devuelto"
    root.overlay(660.0, if (solved) 760.0 else 560.0, dismissOnScrim = false) { dismiss ->
        text(heading, textSize = 32.0, color = Palette.ink) { position(0.0, 0.0) }
        if (solved) {
            val seal = container { position(300.0, 220.0) }
            repeat(12) { index ->
                seal.solidRect(10.0, 150.0, Palette.gold) {
                    anchor(0.5, 1.0)
                    alpha = 0.45
                    rotation = (index * 30).degrees
                }
            }
            seal.solidRect(150.0, 150.0, Palette.gold) {
                anchor(0.5, 0.5)
                rotation = 45.degrees
            }
            seal.text("CERRADO", textSize = 18.0, color = Palette.wax) { position(-52.0, -12.0) }
            seal.rotation = (-8).degrees
        }
        val bodyY = if (solved) 340.0 else 70.0
        blockText(message, 0.0, bodyY, 590.0, 18.0, Palette.ink)
        val buttonY = if (solved) 560.0 else 360.0
        actionButton(
            if (solved) "Volver al escritorio" else "Seguir investigando",
            0.0,
            buttonY,
            460.0,
            64.0,
            if (solved) Palette.greenFelt else Palette.brassDim,
            Palette.paper,
            nav.assets,
            if (solved) ButtonIcons.VOLVER else ButtonIcons.SEGUIR,
        ) {
            dismiss()
            if (solved) nav.go(Route.Desk) else nav.pop()
        }
    }
}

private fun Container.closingTriptych(nav: Navigator, message: String, onDone: () -> Unit) {
    val bmp = nav.assets.get(CLOSING_IMAGE)
    if (bmp == null || bmp.width < 3 || bmp.height < 1) {
        overlay(660.0, 760.0, dismissOnScrim = false) { dismiss ->
            text("Caso cerrado", textSize = 32.0, color = Palette.ink) { position(0.0, 0.0) }
            blockText(message, 0.0, 70.0, 590.0, 18.0, Palette.ink)
            actionButton("Volver al escritorio", 0.0, 560.0, 460.0, 64.0, Palette.greenFelt, Palette.paper, nav.assets, ButtonIcons.VOLVER) {
                dismiss()
                onDone()
            }
        }
        return
    }

    val layer = container()
    var elapsed = 0.0
    var revealed = false
    layer.solidRect(Desk.W, Desk.H, Palette.mahoganyDeep) {
        onClick { if (!revealed) elapsed = TEXT_AT }
    }

    val frameW = 664.0
    val frameH = frameW * bmp.height / bmp.width
    val frameX = (Desk.W - frameW) / 2.0
    val frameY = 136.0
    layer.solidRect(frameW + 16.0, frameH + 16.0, Palette.gold) {
        position(frameX - 8.0, frameY - 8.0)
        mouseEnabled = false
    }
    val stage = layer.fixedSizeContainer(Size(frameW, frameH), clip = true) {
        position(frameX, frameY)
        mouseEnabled = false
    }
    val cuts = if (bmp.width == 1024) intArrayOf(0, 341, 683, 1024) else {
        val third = bmp.width / 3
        intArrayOf(0, third, third * 2, bmp.width)
    }
    val fit = frameW / bmp.width
    val curtains = ArrayList<Container>(3)
    for (index in 0 until 3) {
        val srcX = cuts[index]
        val panelX = srcX * fit
        val panelW = (cuts[index + 1] - srcX) * fit
        stage.fixedSizeContainer(Size(panelW, frameH), clip = true) {
            position(panelX, 0.0)
            mouseEnabled = false
            image(bmp) {
                scale(fit)
                position(-srcX * fit, 0.0)
                mouseEnabled = false
            }
        }
        curtains += stage.container {
            position(panelX, 0.0)
            mouseEnabled = false
            solidRect(panelW, frameH, Colors["#120C08"]) { mouseEnabled = false }
            solidRect(panelW, 5.0, Palette.gold) {
                position(0.0, frameH - 5.0)
                mouseEnabled = false
            }
        }
    }

    val copy = layer.container {
        position(28.0, frameY + frameH + 32.0)
        alpha = 0.0
        mouseEnabled = false
    }
    copy.text("Caso cerrado", textSize = 32.0, color = Palette.gold) { position(0.0, 0.0) }
    val textH = copy.blockText(message, 0.0, 52.0, 664.0, 18.0, Palette.paper)
    copy.actionButton(
        "Volver al escritorio",
        0.0,
        52.0 + textH + 28.0,
        460.0,
        64.0,
        Palette.greenFelt,
        Palette.paper,
        nav.assets,
        ButtonIcons.VOLVER,
    ) { onDone() }

    var textElapsed = 0.0
    layer.addUpdater { dt ->
        val step = dt.toDouble(DurationUnit.SECONDS).coerceAtMost(0.05)
        elapsed += step
        curtains.forEachIndexed { index, curtain ->
            val progress = ((elapsed - REVEAL_AT[index]) / WIPE_SECONDS).coerceIn(0.0, 1.0)
            val eased = progress * progress * (3.0 - 2.0 * progress)
            curtain.y = -frameH * eased
        }
        if (elapsed < TEXT_AT) return@addUpdater
        revealed = true
        textElapsed = (textElapsed + step).coerceAtMost(0.45)
        copy.alpha = textElapsed / 0.45
        if (copy.alpha >= 1.0) copy.mouseEnabled = true
    }
}

private val REVEAL_AT = doubleArrayOf(0.45, 1.45, 2.45)
private const val WIPE_SECONDS = 0.7
private const val TEXT_AT = 2.45 + 0.7 + 0.4
