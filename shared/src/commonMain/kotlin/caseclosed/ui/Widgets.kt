package caseclosed.ui

import caseclosed.shared.generated.resources.Res
import korlibs.image.bitmap.Bitmap
import korlibs.image.color.Colors
import korlibs.image.format.readBitmap
import korlibs.io.file.VfsFile
import korlibs.io.stream.openAsync
import korlibs.korge.input.draggableCloseable
import korlibs.korge.input.onClick
import korlibs.korge.ui.uiTextInput
import korlibs.korge.view.Container
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
    onPress: () -> Unit,
) {
    container {
        position(x, y)
        solidRect(w, h, fill)
        text(label, textSize = 18.0, color = ink) { position(14.0, (h - 22.0) / 2.0) }
        hitRect(w, h, onPress)
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
    solidRect(Desk.W, Desk.HEADER, Palette.mahoganyDeep)
    solidRect(Desk.W, 5.0, Palette.brass) { position(0.0, Desk.HEADER - 5.0) }
    text(clipText(subtitle, 48), textSize = 14.0, color = Palette.brass) { position(24.0, 18.0) }
    text(clipText(title, 28), textSize = 30.0, color = Palette.paper) { position(24.0, 46.0) }
    if (back && nav.canPop()) {
        actionButton("Volver", Desk.W - 148.0, 40.0, 120.0, 52.0, Palette.brassDim, Palette.paper) {
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
    onDone: (note: String, pin: Boolean) -> Unit,
) {
    overlay(660.0, 560.0, dismissOnScrim = true) { dismiss ->
        text(title, textSize = 26.0, color = Palette.ink) { position(0.0, 0.0) }
        blockText(body, 0.0, 44.0, 600.0, 16.0, Palette.inkSoft)
        val input = uiTextInput(initial, Size(600.0, 120.0)) {
            position(0.0, 160.0)
            textSize = 20.0
        }
        actionButton("Guardar nota", 0.0, 310.0, 280.0, 56.0, Palette.greenFelt, Palette.paper) {
            val note = input.text
            dismiss()
            onDone(note, false)
        }
        if (allowPin) {
            actionButton("Guardar y fijar", 300.0, 310.0, 280.0, 56.0, Palette.wax, Palette.paper) {
                val note = input.text
                dismiss()
                onDone(note, true)
            }
        }
        actionButton("Cancelar", 0.0, 384.0, 200.0, 52.0, Palette.brassDim, Palette.paper) { dismiss() }
    }
}

fun Container.confirmDialog(title: String, body: String, confirmLabel: String, onConfirm: () -> Unit) {
    overlay(640.0, 460.0, dismissOnScrim = false) { dismiss ->
        text(title, textSize = 28.0, color = Palette.ink) { position(0.0, 0.0) }
        blockText(body, 0.0, 56.0, 580.0, 20.0, Palette.ink)
        actionButton("Cancelar", 0.0, 280.0, 250.0, 60.0, Palette.brassDim, Palette.paper) { dismiss() }
        actionButton(confirmLabel, 280.0, 280.0, 280.0, 60.0, Palette.wax, Palette.paper) {
            dismiss()
            onConfirm()
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
