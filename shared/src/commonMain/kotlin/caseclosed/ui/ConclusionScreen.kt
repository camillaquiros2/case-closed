package caseclosed.ui

import caseclosed.data.CaseCatalog
import caseclosed.model.Verdict
import korlibs.image.color.Colors
import korlibs.korge.input.onClick
import korlibs.korge.view.Container
import korlibs.korge.view.View
import korlibs.korge.view.alpha
import korlibs.korge.view.anchor
import korlibs.korge.view.container
import korlibs.korge.view.position
import korlibs.korge.view.rotation
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import korlibs.math.geom.degrees

fun buildConclusion(root: Container, nav: Navigator) {
    val engine = nav.engine
    val case = engine.activeCase()
    val nodes = engine.investigation().boardNodes
    root.solidRect(Desk.W, Desk.H, Palette.mahoganyDeep)
    root.chrome("Acusación", case.title, nav, true)

    val accused = arrayOfNulls<String>(1)
    val picked = linkedSetOf<String>()
    val suspectMarks = mutableMapOf<String, View>()
    val evidenceMarks = mutableMapOf<String, View>()

    root.scrollBox(16.0, Desk.HEADER + 8.0, 688.0, Desk.H - Desk.HEADER - 150.0, 0.0, onScroll = {}) {
        var y = 8.0
        text("Paso 1 · Nombra al acusado", textSize = 22.0, color = Palette.paper) { position(8.0, y) }
        y += 40.0
        for (suspect in case.suspects) {
            val row = y
            solidRect(660.0, 72.0, Palette.paper) { position(8.0, row) }
            solidRect(28.0, 28.0, Palette.paperShade) { position(20.0, row + 22.0) }
            val mark = solidRect(18.0, 18.0, Palette.wax) {
                position(25.0, row + 27.0)
                alpha = 0.0
            }
            suspectMarks[suspect.id] = mark
            text(suspect.name, textSize = 20.0, color = Palette.ink) { position(64.0, row + 14.0) }
            text(suspect.alias, textSize = 14.0, color = Palette.inkSoft) { position(64.0, row + 40.0) }
            solidRect(660.0, 72.0, Colors.WHITE) {
                position(8.0, row)
                alpha = 0.01
                onClick {
                    accused[0] = suspect.id
                    for ((id, box) in suspectMarks) box.alpha = if (id == suspect.id) 1.0 else 0.0
                }
            }
            y += 84.0
        }
        y += 8.0
        text("Paso 2 · Pruebas del tablero", textSize = 22.0, color = Palette.paper) { position(8.0, y) }
        y += 36.0
        text(
            "Solo se archivan las fichas que ya están en el corcho. El jefe pide al menos ${case.solution.minEvidenceCount}.",
            textSize = 15.0,
            color = Palette.brass,
        ) { position(8.0, y) }
        y += 48.0
        if (nodes.isEmpty()) {
            y += blockText("El tablero está vacío. Fija tus pruebas antes de archivar.", 8.0, y, 640.0, 18.0, Palette.paper)
        }
        for (node in nodes) {
            val row = y
            val title = when (node.type) {
                "suspect" -> case.suspects.find { it.id == node.entityId }?.name ?: "Sospechoso"
                "item" -> case.items.find { it.id == node.entityId }?.name ?: "Objeto"
                else -> "Cita"
            }
            val excerpt = node.selectedExcerpt ?: ""
            solidRect(660.0, 78.0, Palette.paper) { position(8.0, row) }
            solidRect(28.0, 28.0, Palette.paperShade) { position(20.0, row + 24.0) }
            val mark = solidRect(18.0, 18.0, Palette.wax) {
                position(25.0, row + 29.0)
                alpha = 0.0
            }
            evidenceMarks[node.id] = mark
            text(title, textSize = 18.0, color = Palette.ink) { position(64.0, row + 10.0) }
            text(clipText(excerpt, 62), textSize = 14.0, color = Palette.inkSoft) { position(64.0, row + 40.0) }
            solidRect(660.0, 78.0, Colors.WHITE) {
                position(8.0, row)
                alpha = 0.01
                onClick {
                    if (node.id in picked) picked.remove(node.id) else picked.add(node.id)
                    mark.alpha = if (node.id in picked) 1.0 else 0.0
                }
            }
            y += 90.0
        }
        y + 20.0
    }

    root.actionButton("Presentar acusación", 24.0, Desk.H - 110.0, 672.0, 72.0, Palette.wax, Palette.paper) {
        val suspectId = accused[0]
        if (suspectId == null) {
            root.confirmDialog(
                "Sin nombre en el expediente",
                "Elige a la persona que acusas antes de enviar esto arriba.",
                "Volver",
            ) {}
            return@actionButton
        }
        val name = case.suspects.first { it.id == suspectId }.name
        root.confirmDialog(
            "¿Estás seguro?",
            "¿Archivar esta acusación contra $name? Un nombre equivocado queda en tu expediente. Si el jefe la rechaza, el tablero se queda como está.",
            "Presentar",
        ) {
            val verdict = engine.evaluate(suspectId, picked)
            presentVerdict(root, nav, verdict)
        }
    }
}

private fun presentVerdict(root: Container, nav: Navigator, verdict: Verdict) {
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
            420.0,
            60.0,
            if (solved) Palette.greenFelt else Palette.brassDim,
            Palette.paper,
        ) {
            dismiss()
            if (solved) nav.go(Route.Desk) else nav.pop()
        }
    }
}
