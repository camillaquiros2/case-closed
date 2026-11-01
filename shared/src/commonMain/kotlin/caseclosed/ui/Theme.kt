package caseclosed.ui

import korlibs.image.color.Colors

object Desk {
    const val W = 720.0
    const val H = 1280.0
    const val HEADER = 118.0
}

object Palette {
    val void = Colors["#140D0A"]
    val mahogany = Colors["#3B2214"]
    val mahoganyDeep = Colors["#2A160C"]
    val grain = Colors["#2E1C10"]
    val brass = Colors["#C4A35A"]
    val brassDim = Colors["#8C6B2F"]
    val paper = Colors["#F3E6C8"]
    val paperShade = Colors["#E7D3A8"]
    val highlight = Colors["#E6C35C"]
    val ink = Colors["#1C140E"]
    val inkSoft = Colors["#5C4636"]
    val manila = Colors["#C4A06A"]
    val cork = Colors["#8D6244"]
    val corkDark = Colors["#6E4A32"]
    val yarn = Colors["#9B1B1B"]
    val yarnShadow = Colors["#4A0E0E"]
    val wax = Colors["#7E1A1A"]
    val gold = Colors["#E0B44A"]
    val greenFelt = Colors["#1E3A2F"]
    val phone = Colors["#1A2420"]
    val dossier = Colors["#6B4423"]
    val locker = Colors["#3E3428"]
}

fun wrapLines(text: String, maxWidth: Double, textSize: Double): List<String> {
    val maxChars = (maxWidth / (textSize * 0.50)).toInt().coerceAtLeast(8)
    val paragraphs = text.replace("\r", "").split('\n')
    val lines = mutableListOf<String>()
    for (paragraph in paragraphs) {
        val words = paragraph.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) {
            lines += ""
            continue
        }
        var current = ""
        for (word in words) {
            val trial = if (current.isEmpty()) word else "$current $word"
            if (trial.length > maxChars && current.isNotEmpty()) {
                lines += current
                current = word
            } else {
                current = trial
            }
        }
        if (current.isNotEmpty()) lines += current
    }
    return lines.ifEmpty { listOf("") }
}

fun clipText(text: String, max: Int): String =
    if (text.length <= max) text else text.take(max - 1).trimEnd() + "…"

fun sentencesOf(content: String): List<String> =
    content.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }

fun wordWrapHeight(lineCount: Int, textSize: Double): Double = lineCount * textSize * 1.28
