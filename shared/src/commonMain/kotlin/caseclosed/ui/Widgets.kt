package caseclosed.ui

import caseclosed.data.ButtonIcons
import caseclosed.shared.generated.resources.Res
import korlibs.image.bitmap.Bitmap
import korlibs.image.color.Colors
import korlibs.image.format.readBitmap
import korlibs.io.file.VfsFile
import korlibs.io.stream.openAsync
import korlibs.korge.input.draggableCloseable
import korlibs.korge.input.onClick
import korlibs.korge.input.onOut
import korlibs.korge.input.onOver
import korlibs.korge.ui.uiTextInput
import korlibs.korge.view.Container
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
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min
import kotlin.time.DurationUnit
import korlibs.korge.view.alpha

class AssetBank(private val vfs: VfsFile? = null) {
    private val cache = mutableMapOf<String, Bitmap?>()

    suspend fun preload(paths: List<String>) {
        for (path in paths) cache[path] = load(path)
    }

    fun get(path: String?): Bitmap? = if (path.isNullOrBlank()) null else cache[path]

    private suspend fun load(path: String): Bitmap? = try {
        val bytes = Res.readBytes("files/$path")
        bytes.openAsync().readBitmap()
    } catch (_: Throwable) {
        null
    }
}

class Scroller(
    val content: Container,
    val viewH: Double,
    var contentH: Double,
    var offset: Double,
    private val onScroll: (Double) -> Unit,
) {
    fun set(value: Double, publish: Boolean = false) {
        val max = (contentH - viewH).coerceAtLeast(0.0)
        offset = value.coerceIn(0.0, max)
        content.y = -offset
        if (publish) onScroll(offset)
    }

    fun by(delta: Double) = set(offset + delta, publish = true)
}

fun Container.clearChildren() {
    while (numChildren > 0) getChildAt(0).removeFromParent()
}

fun Container.fadeIn(durationSeconds: Double = 0.24) {
    val duration = durationSeconds.coerceAtLeast(0.01)
    var elapsed = 0.0

    alpha = 0.0

    addUpdater { dt ->
        elapsed = (elapsed + dt.toDouble(DurationUnit.SECONDS))
            .coerceAtMost(duration)

        alpha = (elapsed / duration).coerceIn(0.0, 1.0)
    }
}

fun Container.slideFadeIn(
    fromY: Double = 18.0,
    durationSeconds: Double = 0.28,
) {
    val duration = durationSeconds.coerceAtLeast(0.01)
    val targetY = y
    var elapsed = 0.0

    alpha = 0.0
    y = targetY + fromY

    addUpdater { dt ->
        elapsed = (elapsed + dt.toDouble(DurationUnit.SECONDS))
            .coerceAtMost(duration)

        val progress = (elapsed / duration).coerceIn(0.0, 1.0)
        val eased = progress * progress * (3.0 - 2.0 * progress)

        y = targetY + fromY * (1.0 - eased)
        alpha = eased
    }
}

fun Container.framedRect(w: Double, h: Double, fill: RGBAFill, frame: RGBAFill = Palette.brass) {
    solidRect(w, h, frame)
    solidRect(w - 8, h - 8, fill) { position(4, 4) }
}

typealias RGBAFill = korlibs.image.color.RGBA

fun Container.hitRect(w: Double, h: Double, onPress: () -> Unit) {
    solidRect(w, h, Colors.WHITE) {
        alpha = 0.01
        onClick { onPress() }
    }
}

fun Container.actionButton(
    label: String,
    x: Double,
    y: Double,
    w: Double,
    h: Double,
    fill: RGBAFill,
    ink: RGBAFill,
    assets: AssetBank,
    icon: String,
    onPress: () -> Unit,
) {
    val selected = fill == Palette.gold
    val frame = if (selected) Palette.gold else Palette.brass
    val plate = if (selected) Colors["#8E2E2E"] else Palette.wax
    val button = container { position(x, y) }
    button.solidRect(w, h, frame)
    button.solidRect(w - 6.0, h - 6.0, plate) { position(3.0, 3.0) }
    val iconBox = (h - 16.0).coerceIn(26.0, 52.0)
    val iconX = 8.0
    val iconY = (h - iconBox) / 2.0
    button.buttonLogo(assets, icon, iconX, iconY, iconBox)
    val dividerX = iconX + iconBox + 8.0
    button.solidRect(2.0, (h - 18.0).coerceAtLeast(12.0), Palette.paper) {
        position(dividerX, (h - (h - 18.0).coerceAtLeast(12.0)) / 2.0)
        alpha = 0.9
        mouseEnabled = false
    }
    val textX = dividerX + 12.0
    val textSize = if (h < 56.0) 15.0 else 18.0
    val maxChars = ((w - textX - 8.0) / (textSize * 0.52)).toInt().coerceAtLeast(4)
    button.text(clipText(label, maxChars), textSize = textSize, color = Palette.paper) {
        position(textX, (h - textSize - 4.0) / 2.0)
    }
    var hovering = false
    var shown = 0.0
    button.solidRect(w, h, Colors.WHITE) {
        alpha = 0.01
        onClick { onPress() }
        onOver { hovering = true }
        onOut { hovering = false }
    }
    button.addUpdater { dt ->
        val step = dt.toDouble(DurationUnit.SECONDS).coerceAtMost(0.05)
        val target = if (hovering) 1.0 else 0.0
        shown += (target - shown) * (step * 14.0).coerceAtMost(1.0)
        button.y = y - shown * 3.0
    }
}

private fun Container.buttonLogo(assets: AssetBank, path: String, x: Double, y: Double, size: Double) {
    val bmp = assets.get(path)
    if (bmp != null) {
        val bw = bmp.width.toDouble().coerceAtLeast(1.0)
        val bh = bmp.height.toDouble().coerceAtLeast(1.0)
        val fitted = min(size / bw, size / bh)
        val dw = bw * fitted
        val dh = bh * fitted
        image(bmp) {
            position(x + (size - dw) / 2.0, y + (size - dh) / 2.0)
            scale(fitted)
            mouseEnabled = false
        }
    } else {
        solidRect(size, size, Palette.paper) {
            position(x, y)
            alpha = 0.16
            mouseEnabled = false
        }
        solidRect(size * 0.42, size * 0.5, Palette.paper) {
            position(x + size * 0.29, y + size * 0.24)
            alpha = 0.72
            mouseEnabled = false
        }
    }
}

fun Container.blockText(
    content: String,
    x: Double,
    y: Double,
    width: Double,
    size: Double,
    color: RGBAFill,
): Double {
    val lines = wrapLines(content, width, size)
    val lineH = size * 1.28
    lines.forEachIndexed { index, line ->
        text(line, textSize = size, color = color) { position(x, y + index * lineH) }
    }
    return lines.size * lineH
}

fun Container.photo(bmp: Bitmap?, w: Double, h: Double, label: String, tint: RGBAFill) {
    if (bmp != null) {
        image(bmp) {
            scale(w / bmp.width.toDouble(), h / bmp.height.toDouble())
        }
    } else {
        solidRect(w, h, tint)
        solidRect(w, 4.0, Palette.brass)
        solidRect(4.0, h, Palette.brass)
        val lines = wrapLines(label, w - 16.0, 15.0).take(5)
        lines.forEachIndexed { index, line ->
            text(line, textSize = 15.0, color = Palette.paper) { position(10.0, 12.0 + index * 20.0) }
        }
    }
}

fun Container.scrollBox(
    x: Double,
    y: Double,
    w: Double,
    h: Double,
    initial: Double,
    onScroll: (Double) -> Unit,
    builder: Container.() -> Double,
): Scroller {
    lateinit var scroller: Scroller
    fixedSizeContainer(Size(w, h), clip = true) {
        position(x, y)
        val content = container()
        val contentH = content.builder()
        scroller = Scroller(content, h, contentH, 0.0, onScroll)
        scroller.set(initial)
        content.draggableCloseable(autoMove = false) { info ->
            val proposed = info.viewNextXY.y.toDouble()
            val max = (contentH - h).coerceAtLeast(0.0)
            val nextY = proposed.coerceIn(-max, 0.0)
            content.y = nextY
            scroller.offset = -nextY
            onScroll(scroller.offset)
        }
    }
    return scroller
}


fun Container.chrome(title: String, subtitle: String, nav: Navigator, back: Boolean) {

    // Fondo del encabezado
    solidRect(Desk.W, Desk.HEADER, Palette.mahoganyDeep)

    // Marco dorado exterior
    solidRect(Desk.W - 16.0, 2.0, Palette.brass) {
        position(8.0, 7.0)
    }
    solidRect(Desk.W - 16.0, 2.0, Palette.brass) {
        position(8.0, Desk.HEADER - 9.0)
    }
    solidRect(2.0, Desk.HEADER - 16.0, Palette.brass) {
        position(8.0, 8.0)
    }
    solidRect(2.0, Desk.HEADER - 16.0, Palette.brass) {
        position(Desk.W - 10.0, 8.0)
    }

    // Subtítulo
    text(
        clipText(subtitle, 35),
        textSize = 14.0,
        color = Palette.brass
    ) {
        position(48.0, 17.0)
        mouseEnabled = false
    }

    // Icono y línea divisoria vertical
    buttonLogo(
        nav.assets,
        ButtonIcons.CASOS,
        36.0,
        43.0,
        34.0
    )

    solidRect(2.0, 40.0, Palette.brass) {
        position(76.0, 40.0)
    }

    // Título principal
    text(
        clipText(title, 27),
        textSize = 30.0,
        color = Palette.paper
    ) {
        position(88.0, 42.0)
        mouseEnabled = false
    }

    // Línea izquierda (longitud exacta de 95.0)
    solidRect(95.0, 2.0, Palette.gold) {
        position(27.0, 87.0)
        mouseEnabled = false
    }

    // Rombo central perfectamente centrado (alineado al eje Y de las líneas)
    solidRect(6.0, 6.0, Palette.gold) {
        position(124.0, 85.0)
        rotation = 45.degrees
        mouseEnabled = false
    }

    // Línea derecha (longitud exacta de 95.0, simétrica a la izquierda)
    solidRect(95.0, 2.0, Palette.gold) {
        position(130.0, 87.0)
        mouseEnabled = false
    }

    // Botón de Volver opcional
    if (back && nav.canPop()) {
        actionButton(
            "Volver",
            Desk.W - 168.0,
            36.0,
            140.0,
            56.0,
            Palette.brassDim,
            Palette.paper,
            nav.assets,
            ButtonIcons.VOLVER
        ) {
            nav.pop()
        }
    }
}
fun Container.woodBackground(assets: AssetBank) {
    val bmp = assets.get("images/desk/desk_wood_bg.png")
    if (bmp != null) {
        image(bmp) { scale(Desk.W / bmp.width.toDouble(), Desk.H / bmp.height.toDouble()) }
    } else {
        solidRect(Desk.W, Desk.H, Palette.mahogany)
        var y = 0.0
        while (y < Desk.H) {
            solidRect(Desk.W, 2.0, Palette.grain) { position(0.0, y) }
            y += 28.0
        }
    }
    solidRect(Desk.W, 220.0, Colors["#66FFE2A8"]) { alpha = 0.18 }
}

fun Container.overlay(
    panelW: Double,
    panelH: Double,
    dismissOnScrim: Boolean,
    block: Container.(dismiss: () -> Unit) -> Unit,
) {
    val layer = container()
    layer.solidRect(Desk.W, Desk.H, Colors.BLACK) {
        alpha = 0.62
        if (dismissOnScrim) onClick { layer.removeFromParent() }
    }
    val panel = layer.container { position((Desk.W - panelW) / 2.0, (Desk.H - panelH) / 2.0) }
    panel.solidRect(panelW, panelH, Palette.brass)
    panel.solidRect(panelW - 12.0, panelH - 12.0, Palette.paper) { position(6.0, 6.0) }
    val inner = panel.container { position(28.0, 28.0) }
    inner.block { layer.removeFromParent() }
}

fun Container.noteEditor(
    title: String,
    body: String,
    initial: String,
    allowPin: Boolean,
    assets: AssetBank,
    onDone: (note: String, pin: Boolean) -> Unit,
) {
    overlay(660.0, 560.0, dismissOnScrim = true) { dismiss ->
        text(title, textSize = 26.0, color = Palette.ink) { position(0.0, 0.0) }
        blockText(body, 0.0, 44.0, 600.0, 16.0, Palette.inkSoft)
        val input = uiTextInput(initial, Size(600.0, 120.0)) {
            position(0.0, 160.0)
            textSize = 20.0
        }
        actionButton("Guardar nota", 0.0, 310.0, 280.0, 56.0, Palette.greenFelt, Palette.paper, assets, ButtonIcons.GUARDAR) {
            val note = input.text
            dismiss()
            onDone(note, false)
        }
        if (allowPin) {
            actionButton("Guardar y fijar", 300.0, 310.0, 280.0, 56.0, Palette.wax, Palette.paper, assets, ButtonIcons.FIJAR) {
                val note = input.text
                dismiss()
                onDone(note, true)
            }
        }
        actionButton("Cancelar", 0.0, 384.0, 220.0, 52.0, Palette.brassDim, Palette.paper, assets, ButtonIcons.CANCELAR) { dismiss() }
    }
}

fun Container.confirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    assets: AssetBank,
    confirmIcon: String = ButtonIcons.CONFIRMAR,
    onConfirm: () -> Unit,
) {
    overlay(640.0, 460.0, dismissOnScrim = false) { dismiss ->
        text(title, textSize = 28.0, color = Palette.ink) { position(0.0, 0.0) }
        blockText(body, 0.0, 56.0, 580.0, 20.0, Palette.ink)
        actionButton("Cancelar", 0.0, 280.0, 250.0, 60.0, Palette.brassDim, Palette.paper, assets, ButtonIcons.CANCELAR) { dismiss() }
        actionButton(confirmLabel, 280.0, 280.0, 280.0, 60.0, Palette.wax, Palette.paper, assets, confirmIcon) {
            onConfirm()
            dismiss()
        }
    }
}

fun Container.drawPolyline(points: List<Vec>, color: RGBAFill, thickness: Double, ox: Double, oy: Double) {
    for (i in 0 until points.lastIndex) {
        val a = points[i]
        val b = points[i + 1]
        val dx = b.x - a.x
        val dy = b.y - a.y
        val len = hypot(dx, dy).coerceAtLeast(0.6)
        val deg = atan2(dy, dx) * 180.0 / PI
        solidRect(len, thickness, color) {
            anchor(0.0, 0.5)
            position(a.x + ox, a.y + oy)
            rotation = deg.degrees
            mouseEnabled = false
        }
    }
}

fun Container.sentenceBlock(
    sentence: String,
    note: String?,
    width: Double,
    y: Double,
    onTap: () -> Unit,
): Double {
    val lines = wrapLines(sentence, width - 24.0, 18.0)
    val noteLines = if (note.isNullOrBlank()) emptyList() else wrapLines("Nota: $note", width - 24.0, 14.0)
    val h = 14.0 + lines.size * 23.0 + noteLines.size * 18.0 + 10.0
    solidRect(width, h, if (note != null) Palette.highlight else Palette.paper) { position(0.0, y) }
    lines.forEachIndexed { index, line ->
        text(line, textSize = 18.0, color = Palette.ink) { position(12.0, y + 10.0 + index * 23.0) }
    }
    noteLines.forEachIndexed { index, line ->
        text(line, textSize = 14.0, color = Palette.inkSoft) {
            position(12.0, y + 12.0 + lines.size * 23.0 + index * 18.0)
        }
    }
    solidRect(width, h, Colors.WHITE) {
        position(0.0, y)
        alpha = 0.01
        onClick { onTap() }
    }
    return h + 8.0
}
