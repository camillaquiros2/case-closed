package caseclosed.ui

import caseclosed.data.ButtonIcons
import korlibs.korge.view.Container
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.korge.view.position
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import kotlin.time.DurationUnit

/**
 * Case story shown like a game intro: the briefing appears one sentence at a time.
 * Tap or Siguiente finishes the sentence being typed, or shows the next one.
 * Opens after Iniciar juego and from the case header on the desk.
 * It reads the briefing from the case data, so new cases need no code.
 */
fun buildCaseIntro(root: Container, nav: Navigator) {
    val case = nav.engine.activeCase()
    val sentences = sentencesOf(case.briefing).ifEmpty { listOf(case.briefing) }
    val left = 56.0
    val width = Desk.W - left * 2
    val storyTop = 470.0
    val storyBottom = 1010.0
    val storySize = 24.0
    val lineH = wordWrapHeight(1, storySize)
    val sentenceGap = 18.0
    val charsPerSecond = 45.0

    menuBackdrop(root, nav.assets, case.scenes.firstOrNull()?.image)

    root.text(case.caseNumber, textSize = 18.0, color = Palette.brass) { position(left, 200.0) }
    val titleH = root.blockText(case.title, left, 232.0, width, 40.0, Palette.paper)
    val ruleY = 232.0 + titleH + 18.0
    root.solidRect(160.0, 4.0, Palette.brass) { position(left, ruleY) }
    root.text(clipText("${case.location}  ·  ${case.date}", 56), textSize = 17.0, color = Palette.paperShade) {
        position(left, ruleY + 22.0)
    }

    val story = root.container()
    val progress = root.text("", textSize = 16.0, color = Palette.brass) { position(left, storyBottom + 20.0) }

    var index = 0
    var typed = 0.0
    var drawnChars = -1

    fun currentLength(): Int = sentences[index].length
    fun isDone(): Boolean = index == sentences.lastIndex && typed >= currentLength()

    fun finish() {
        if (nav.canPop()) nav.pop() else nav.go(Route.Desk)
    }

    /** Current sentence in full color, earlier ones dimmed above it while they fit. */
    fun draw() {
        story.clearChildren()
        val shownChars = typed.toInt().coerceAtMost(currentLength())
        drawnChars = shownChars
        val currentLines = wrapLines(sentences[index], width, storySize)
        val blocks = mutableListOf(currentLines to true)
        var used = currentLines.size * lineH
        var previous = index - 1
        while (previous >= 0) {
            val lines = wrapLines(sentences[previous], width, storySize)
            val h = lines.size * lineH + sentenceGap
            if (used + h > storyBottom - storyTop) break
            used += h
            blocks.add(0, lines to false)
            previous -= 1
        }
        var y = storyTop
        for ((lines, current) in blocks) {
            var remaining = if (current) shownChars else Int.MAX_VALUE
            for (line in lines) {
                if (remaining <= 0) break
                val visible = if (remaining >= line.length) line else line.take(remaining)
                remaining -= line.length + 1
                story.text(visible, textSize = storySize, color = if (current) Palette.paper else Palette.paperShade) {
                    position(left, y)
                    if (!current) alpha = 0.55
                }
                y += lineH
            }
            if (!current) y += sentenceGap
        }
        progress.text = "${index + 1} / ${sentences.size}"
    }

    val buttonY = storyBottom + 70.0
    val buttonH = 76.0
    val skipW = 220.0
    val nextX = left + skipW + 20.0
    val nextW = width - skipW - 20.0
    val nextBox = root.container()
    val doneBox = root.container()

    fun showButtons() {
        nextBox.visible = !isDone()
        doneBox.visible = isDone()
    }

    fun advance() {
        when {
            typed < currentLength() -> typed = currentLength().toDouble()
            index < sentences.lastIndex -> {
                index += 1
                typed = 0.0
            }
            else -> {
                finish()
                return
            }
        }
        draw()
        showButtons()
    }

    root.container().hitRect(Desk.W, buttonY - 10.0) { advance() }

    root.actionButton(GameText.INTRO_SKIP, left, buttonY, skipW, buttonH, Palette.brassDim, Palette.paper, nav.assets, ButtonIcons.SEGUIR) {
        finish()
    }
    nextBox.actionButton(GameText.INTRO_NEXT, nextX, buttonY, nextW, buttonH, Palette.wax, Palette.paper, nav.assets, ButtonIcons.SIGUIENTE) {
        advance()
    }
    doneBox.actionButton(GameText.INTRO_TO_DESK, nextX, buttonY, nextW, buttonH, Palette.wax, Palette.paper, nav.assets, ButtonIcons.ABRIR) {
        finish()
    }

    draw()
    showButtons()

    story.addUpdater { dt ->
        if (typed < currentLength()) {
            typed = (typed + dt.toDouble(DurationUnit.SECONDS) * charsPerSecond).coerceAtMost(currentLength().toDouble())
            if (typed.toInt() != drawnChars) {
                draw()
                showButtons()
            }
        }
    }
}
