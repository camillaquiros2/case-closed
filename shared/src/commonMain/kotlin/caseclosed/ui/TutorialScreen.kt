package caseclosed.ui

import caseclosed.data.ButtonIcons
import caseclosed.data.CaseArt
import caseclosed.data.DeskImages
import caseclosed.data.TutorialStep
import caseclosed.data.TutorialSteps
import caseclosed.model.CaseData
import korlibs.image.bitmap.Bitmap
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.fixedSizeContainer
import korlibs.korge.view.image
import korlibs.korge.view.position
import korlibs.korge.view.scale
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import korlibs.math.geom.Size
import kotlin.math.max
import kotlin.math.min

/**
 * How to play, one part of the desk per step, like the first screens of a game.
 * Same look as the menu and the case intro: the cover drifts behind a dark veil.
 * Opens by itself on the first launch and from the main menu any time after.
 * Anterior / Siguiente move between steps and Saltar leaves at any moment.
 * Finishing or skipping only records that it was seen. The investigation is not touched.
 * The steps live in [TutorialSteps], so changing the text needs no code here.
 */
fun buildTutorial(root: Container, nav: Navigator) {
    val steps = TutorialSteps.all
    val case = nav.engine.activeCase()
    val caseCover = case.scenes.firstOrNull()?.image
    val left = 56.0
    val width = Desk.W - left * 2
    val frameY = 270.0
    val frameH = 420.0
    val buttonY = 1080.0
    val buttonH = 76.0
    val backW = 220.0

    val cover = if (nav.assets.get(DeskImages.MENU_BACKGROUND) != null) DeskImages.MENU_BACKGROUND else caseCover
    menuBackdrop(root, nav.assets, cover)

    root.text(
        "${GameText.MENU_TUTORIAL.uppercase()}  ·  ${GameText.TUTORIAL_SUBTITLE}",
        textSize = 18.0,
        color = Palette.brass,
    ) { position(left, 118.0) }

    var index = 0
    val page = root.container()

    fun finish() {
        nav.engine.markTutorialSeen()
        nav.post { if (nav.canPop()) nav.pop() else nav.go(Route.Title) }
    }

    fun draw() {
        page.clearChildren()
        val step = steps[index]
        val last = index == steps.lastIndex

        if (!last) {
            page.actionButton(GameText.INTRO_SKIP, Desk.W - left - 160.0, 96.0, 160.0, 60.0, Palette.brassDim, Palette.paper, nav.assets, ButtonIcons.SEGUIR) {
                finish()
            }
        }

        page.text(GameText.tutorialStep(index + 1, steps.size), textSize = 16.0, color = Palette.paperShade) {
            position(left, 196.0)
        }
        tutorialProgress(page, left, 228.0, width, steps.size, index)

        val content = page.container()
        content.x = left
        content.y = frameY
        content.framedRect(width, frameH, Palette.void)

        val boxW = width - 8.0
        val boxH = frameH - 8.0
        val box = content.fixedSizeContainer(Size(boxW, boxH), clip = true) { position(4.0, 4.0) }
        var paths = picturesFor(step, case).filter { nav.assets.get(it) != null }
        var fill = step.caseArt != CaseArt.NONE
        if (paths.isEmpty() && caseCover != null) {
            paths = listOf(caseCover)
            fill = true
        }
        if (paths.isNotEmpty()) {
            val count = paths.size
            val cols = if (count <= 3) count else (count + 1) / 2
            val rows = (count + cols - 1) / cols
            val gap = if (count > 1) 6.0 else 0.0
            val cellW = (boxW - gap * (cols - 1)) / cols
            val cellH = (boxH - gap * (rows - 1)) / rows
            paths.forEachIndexed { i, path ->
                val art = nav.assets.get(path) ?: return@forEachIndexed
                box.fixedSizeContainer(Size(cellW, cellH), clip = true) {
                    position((i % cols) * (cellW + gap), (i / cols) * (cellH + gap))
                    fitArt(art, cellW, cellH, fill)
                }
            }
        }

        val titleY = frameH + 48.0
        val titleH = content.blockText(step.title, 0.0, titleY, width, 40.0, Palette.paper)
        val ruleY = titleY + titleH + 14.0
        content.solidRect(160.0, 4.0, Palette.brass) { position(0.0, ruleY) }
        content.blockText(step.body, 0.0, ruleY + 28.0, width, 24.0, Palette.paperShade)
        content.slideFadeIn()

        if (index > 0) {
            page.actionButton(GameText.TUTORIAL_BACK, left, buttonY, backW, buttonH, Palette.brassDim, Palette.paper, nav.assets, ButtonIcons.ANTERIOR) {
                index -= 1
                nav.post { draw() }
            }
        }
        val nextX = left + backW + 20.0
        val nextW = width - backW - 20.0
        val nextLabel = if (last) GameText.TUTORIAL_DONE else GameText.INTRO_NEXT
        val nextIcon = if (last) ButtonIcons.CONFIRMAR else ButtonIcons.SIGUIENTE
        page.actionButton(nextLabel, nextX, buttonY, nextW, buttonH, Palette.wax, Palette.paper, nav.assets, nextIcon) {
            if (last) {
                finish()
            } else {
                index += 1
                nav.post { draw() }
            }
        }
    }

    draw()
}

/** The step's own pictures, or the ones it borrows from the active case. */
private fun picturesFor(step: TutorialStep, case: CaseData): List<String> = when (step.caseArt) {
    CaseArt.NONE -> step.images
    CaseArt.COVER -> listOfNotNull(case.scenes.firstOrNull()?.image)
    CaseArt.SCENE_ZONES -> case.scenes.firstOrNull()?.subscenes?.map { it.image }.orEmpty()
    CaseArt.SUSPECTS -> case.suspects.map { it.portrait }
}.filter { it.isNotBlank() }

/** Case art fills its cell; desk objects fit whole inside it with a little air around. */
private fun Container.fitArt(art: Bitmap, w: Double, h: Double, fill: Boolean) {
    val aw = art.width.toDouble()
    val ah = art.height.toDouble()
    val fitted = if (fill) max(w / aw, h / ah) else min(w / aw, h / ah) * 0.86
    image(art) {
        scale(fitted)
        position((w - aw * fitted) / 2.0, (h - ah * fitted) / 2.0)
        mouseEnabled = false
    }
}

/** One bar per step. The current one and the ones before it are gold. */
private fun tutorialProgress(root: Container, x: Double, y: Double, width: Double, count: Int, current: Int) {
    val gap = 8.0
    val barW = (width - gap * (count - 1)) / count
    for (i in 0 until count) {
        root.solidRect(barW, 6.0, if (i <= current) Palette.gold else Palette.brassDim) {
            position(x + i * (barW + gap), y)
            if (i > current) alpha = 0.5
        }
    }
}
