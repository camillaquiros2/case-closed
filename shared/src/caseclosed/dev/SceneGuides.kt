package caseclosed.dev

import caseclosed.data.CaseCatalog
import caseclosed.model.CaseData
import caseclosed.model.CrimeScene
import caseclosed.model.ItemOrigin
import caseclosed.model.PercentBox
import caseclosed.model.SubScene
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.image.format.PNG
import korlibs.image.format.encode
import korlibs.io.file.VfsFile
import korlibs.io.file.std.localCurrentDirVfs

/**
 * Layout maps for scene art. While [WRITE_ON_LOAD] is true, launching the game
 * writes `guide.png` into each scene folder. The picture outlines every part of
 * the room and where each object sits, so it can be handed to an image model.
 * Set [WRITE_ON_LOAD] to false when those files should stop refreshing.
 */
object SceneGuides {
    const val WRITE_ON_LOAD = true

    const val MAP_X = 48
    const val MAP_Y = 44
    const val MAP_W = 1184
    const val MAP_H = 640
    const val CANVAS_W = 1280

    private val background = Colors["#241C16"]
    private val ink = Colors["#F3E6C8"]
    private val plate = Colors["#1C140E"]
    private val regionColors = listOf(
        Colors["#E6C35C"],
        Colors["#7EB6D9"],
        Colors["#D97E7E"],
        Colors["#8FBF7A"],
        Colors["#C49AE8"],
        Colors["#E0A15A"],
    )

    fun regionColor(index: Int): RGBA = regionColors[index % regionColors.size]

    suspend fun writeIfEnabled() {
        if (!WRITE_ON_LOAD) return
        try {
            val assets = projectAssets()
            if (assets == null) {
                println("Scene guides skipped: public/assets was not found.")
                return
            }
            var count = 0
            for (pack in CaseCatalog.packs) {
                for (scene in pack.data.scenes) {
                    val relative = "images/cases/${pack.data.id}/scenes/${scene.id}/guide.png"
                    val file = assets[relative]
                    file.parent.mkdirs()
                    file.write(PNG.encode(render(pack.data, scene)))
                    count += 1
                }
            }
            println("Wrote $count scene layout guides into public/assets/images/cases")
        } catch (t: Throwable) {
            println("Scene guide images were not written: ${t.message}")
        }
    }

    fun render(case: CaseData, scene: CrimeScene): Bitmap32 {
        val heights = buildList {
            add(panelHeight(scene.subscenes.size.coerceAtLeast(1)))
            for (sub in scene.subscenes) add(panelHeight(sub.items.size.coerceAtLeast(1)))
        }
        val bmp = Bitmap32(CANVAS_W, heights.sum().coerceAtLeast(1))
        bmp.fillRect(0, 0, bmp.width, bmp.height, background, hatch = false)
        var top = 0
        drawOverhead(bmp, scene, top)
        top += heights[0]
        scene.subscenes.forEachIndexed { index, sub ->
            drawCloseup(bmp, case, sub, top)
            top += heights[index + 1]
        }
        return bmp
    }

    private fun panelHeight(lines: Int): Int = MAP_Y + MAP_H + 16 + lines * 26 + 20

    private fun drawOverhead(bmp: Bitmap32, scene: CrimeScene, top: Int) {
        val mapY = top + MAP_Y
        bmp.drawText(MAP_X, top + 8, ascii("LAYOUT  ${scene.name}  OVERHEAD"), ink, scale = 3)
        scene.subscenes.forEachIndexed { index, sub ->
            val rect = pixelBox(MAP_X, mapY, sub.bounds)
            bmp.framed(rect, regionColor(index), hatch = false)
            bmp.badge(rect.x + 8, rect.y + 8, "${index + 1}")
        }
        var lineY = mapY + MAP_H + 12
        if (scene.subscenes.isEmpty()) {
            bmp.drawText(MAP_X, lineY, "NO PARTS", ink, scale = 2)
            return
        }
        scene.subscenes.forEachIndexed { index, sub ->
            val box = sub.bounds
            val line = "${index + 1}  ${ascii(sub.name)}  ${box.x.toInt()},${box.y.toInt()}  ${box.w.toInt()}x${box.h.toInt()}"
            bmp.drawText(MAP_X, lineY, line, regionColor(index), scale = 2)
            lineY += 26
        }
    }

    private fun drawCloseup(bmp: Bitmap32, case: CaseData, sub: SubScene, top: Int) {
        val mapY = top + MAP_Y
        bmp.drawText(MAP_X, top + 8, ascii(sub.name), ink, scale = 3)
        bmp.fillRect(MAP_X, mapY, MAP_W, MAP_H, Colors["#3A2E24"], hatch = false)
        if (sub.items.isEmpty()) {
            bmp.drawText(MAP_X, mapY + MAP_H + 12, "NOTHING TO COLLECT", ink, scale = 2)
            return
        }
        sub.items.forEachIndexed { index, spot ->
            val item = case.items.firstOrNull { it.id == spot.itemId }
            val irrelevant = item?.relevant == false
            val rect = pixelBox(MAP_X, mapY, spot.bounds)
            bmp.framed(rect, if (irrelevant) Colors["#8A8A8A"] else regionColor(index), hatch = irrelevant)
            bmp.badge(rect.x + 8, rect.y + 8, "${index + 1}")
        }
        var lineY = mapY + MAP_H + 12
        sub.items.forEachIndexed { index, spot ->
            val item = case.items.firstOrNull { it.id == spot.itemId }
            val name = item?.name ?: spot.itemId
            val kind = when {
                item == null -> "MISSING"
                !item.relevant -> "NOT RELEVANT"
                item.origin == ItemOrigin.FILE -> "IN THE OPENING FILES"
                else -> "COLLECT ON SCENE"
            }
            val box = spot.bounds
            val line = "${index + 1}  ${ascii(name)}  $kind  ${box.x.toInt()},${box.y.toInt()}  ${box.w.toInt()}x${box.h.toInt()}"
            val color = if (item?.relevant == false) Colors["#C8C8C8"] else regionColor(index)
            bmp.drawText(MAP_X, lineY, line, color, scale = 2)
            lineY += 26
        }
    }

    private fun pixelBox(originX: Int, originY: Int, box: PercentBox): PixelBox {
        val x = originX + (MAP_W * box.x / 100.0).toInt()
        val y = originY + (MAP_H * box.y / 100.0).toInt()
        val w = (MAP_W * box.w / 100.0).toInt().coerceAtLeast(12)
        val h = (MAP_H * box.h / 100.0).toInt().coerceAtLeast(12)
        return PixelBox(x, y, w, h)
    }

    private suspend fun projectAssets(): VfsFile? {
        var dir = localCurrentDirVfs
        repeat(8) {
            if (dir["settings.gradle.kts"].exists()) return dir["public/assets"]
            val parent = dir.parent
            if (parent.absolutePath == dir.absolutePath) return null
            dir = parent
        }
        return null
    }
}

private data class PixelBox(val x: Int, val y: Int, val w: Int, val h: Int)

private fun Bitmap32.framed(box: PixelBox, color: RGBA, hatch: Boolean) {
    fillRect(box.x, box.y, box.w, box.h, Colors["#140D0A"], hatch = false)
    val inset = 4
    if (box.w > inset * 2 && box.h > inset * 2) {
        fillRect(box.x + inset, box.y + inset, box.w - inset * 2, box.h - inset * 2, color, hatch)
    }
}

private fun Bitmap32.badge(x: Int, y: Int, label: String) {
    fillRect(x, y, 28, 28, Colors["#1C140E"], hatch = false)
    drawText(x + 6, y + 4, label, Colors["#F3E6C8"], scale = 2)
}

private fun Bitmap32.fillRect(x: Int, y: Int, w: Int, h: Int, color: RGBA, hatch: Boolean) {
    val x0 = x.coerceIn(0, width)
    val y0 = y.coerceIn(0, height)
    val x1 = (x + w).coerceIn(0, width)
    val y1 = (y + h).coerceIn(0, height)
    for (yy in y0 until y1) {
        for (xx in x0 until x1) {
            val stripe = hatch && ((xx + yy) / 8) % 2 == 0
            this[xx, yy] = if (stripe) Colors["#1C140E"] else color
        }
    }
}

private fun Bitmap32.drawText(x: Int, y: Int, text: String, color: RGBA, scale: Int) {
    var cursor = x
    val advance = scale * 5 + 2
    for (ch in text) {
        if (cursor > width - advance) break
        val rows = GLYPHS[ch]
        if (rows != null) {
            for (row in rows.indices) {
                val bits = rows[row]
                for (col in 0 until 5) {
                    if ((bits shr (4 - col)) and 1 == 1) {
                        fillRect(cursor + col * scale, y + row * scale, scale, scale, color, hatch = false)
                    }
                }
            }
        }
        if (ch == ' ' || rows != null) cursor += advance
    }
}

private fun ascii(text: String): String = buildString {
    for (ch in text) {
        val mapped = when (ch.lowercaseChar()) {
            'á' -> 'A'
            'é' -> 'E'
            'í' -> 'I'
            'ó' -> 'O'
            'ú', 'ü' -> 'U'
            'ñ' -> 'N'
            else -> ch.uppercaseChar()
        }
        if (mapped == ' ' || mapped == ',' || mapped in GLYPHS) append(mapped)
    }
}

private val GLYPHS: Map<Char, IntArray> = mapOf(
    'A' to g(0b01110, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001),
    'B' to g(0b11110, 0b10001, 0b10001, 0b11110, 0b10001, 0b10001, 0b11110),
    'C' to g(0b01110, 0b10001, 0b10000, 0b10000, 0b10000, 0b10001, 0b01110),
    'D' to g(0b11110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b11110),
    'E' to g(0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b11111),
    'F' to g(0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b10000),
    'G' to g(0b01110, 0b10001, 0b10000, 0b10111, 0b10001, 0b10001, 0b01111),
    'H' to g(0b10001, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001),
    'I' to g(0b01110, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110),
    'J' to g(0b00111, 0b00010, 0b00010, 0b00010, 0b00010, 0b10010, 0b01100),
    'K' to g(0b10001, 0b10010, 0b10100, 0b11000, 0b10100, 0b10010, 0b10001),
    'L' to g(0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b11111),
    'M' to g(0b10001, 0b11011, 0b10101, 0b10101, 0b10001, 0b10001, 0b10001),
    'N' to g(0b10001, 0b11001, 0b10101, 0b10011, 0b10001, 0b10001, 0b10001),
    'O' to g(0b01110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110),
    'P' to g(0b11110, 0b10001, 0b10001, 0b11110, 0b10000, 0b10000, 0b10000),
    'Q' to g(0b01110, 0b10001, 0b10001, 0b10001, 0b10101, 0b10010, 0b01101),
    'R' to g(0b11110, 0b10001, 0b10001, 0b11110, 0b10100, 0b10010, 0b10001),
    'S' to g(0b01111, 0b10000, 0b10000, 0b01110, 0b00001, 0b00001, 0b11110),
    'T' to g(0b11111, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100),
    'U' to g(0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110),
    'V' to g(0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01010, 0b00100),
    'W' to g(0b10001, 0b10001, 0b10001, 0b10101, 0b10101, 0b10101, 0b01010),
    'X' to g(0b10001, 0b10001, 0b01010, 0b00100, 0b01010, 0b10001, 0b10001),
    'Y' to g(0b10001, 0b10001, 0b01010, 0b00100, 0b00100, 0b00100, 0b00100),
    'Z' to g(0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b10000, 0b11111),
    '0' to g(0b01110, 0b10001, 0b10011, 0b10101, 0b11001, 0b10001, 0b01110),
    '1' to g(0b00100, 0b01100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110),
    '2' to g(0b01110, 0b10001, 0b00001, 0b00010, 0b00100, 0b01000, 0b11111),
    '3' to g(0b11110, 0b00001, 0b00001, 0b01110, 0b00001, 0b00001, 0b11110),
    '4' to g(0b10010, 0b10010, 0b10010, 0b11111, 0b00010, 0b00010, 0b00010),
    '5' to g(0b11111, 0b10000, 0b10000, 0b11110, 0b00001, 0b00001, 0b11110),
    '6' to g(0b01110, 0b10000, 0b10000, 0b11110, 0b10001, 0b10001, 0b01110),
    '7' to g(0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b01000, 0b01000),
    '8' to g(0b01110, 0b10001, 0b10001, 0b01110, 0b10001, 0b10001, 0b01110),
    '9' to g(0b01110, 0b10001, 0b10001, 0b01111, 0b00001, 0b00001, 0b01110),
    ',' to g(0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b00110, 0b01100),
    '_' to g(0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b11111),
)

private fun g(vararg rows: Int): IntArray = rows
