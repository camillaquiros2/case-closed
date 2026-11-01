package caseclosed.ui

import kotlin.math.hypot

/**
 * Red-string geometry for the evidence corkboard.
 *
 * Node positions are percentages of the cork rectangle (clamped to 5%–85%).
 * A connection is a quadratic Bezier from pin center to pin center. The control
 * point sits below the midpoint so the yarn sags:
 *
 * B(t) = (1-t)^2 * A + 2(1-t)t * C + t^2 * B
 */
data class Vec(val x: Double, val y: Double)

object Yarn {
    const val MIN_PERCENT = 5.0
    const val MAX_PERCENT = 85.0
    const val HIT_SLOP = 18.0

    fun clampPercent(value: Double): Double = value.coerceIn(MIN_PERCENT, MAX_PERCENT)

    fun pinTopLeft(percentX: Double, percentY: Double, boardW: Double, boardH: Double): Vec =
        Vec((percentX / 100.0) * boardW, (percentY / 100.0) * boardH)

    fun pinCenter(
        percentX: Double,
        percentY: Double,
        boardW: Double,
        boardH: Double,
        cardW: Double,
        cardH: Double,
    ): Vec {
        val topLeft = pinTopLeft(percentX, percentY, boardW, boardH)
        return Vec(topLeft.x + cardW / 2.0, topLeft.y + cardH / 2.0)
    }

    fun percentOf(px: Double, py: Double, boardW: Double, boardH: Double): Pair<Double, Double> {
        val x = if (boardW <= 0.0) MIN_PERCENT else clampPercent(px / boardW * 100.0)
        val y = if (boardH <= 0.0) MIN_PERCENT else clampPercent(py / boardH * 100.0)
        return x to y
    }

    fun controlPoint(a: Vec, b: Vec): Vec {
        val sag = (hypot(b.x - a.x, b.y - a.y) * 0.15).coerceIn(14.0, 56.0)
        return Vec((a.x + b.x) / 2.0, (a.y + b.y) / 2.0 + sag)
    }

    fun pointOnQuad(a: Vec, control: Vec, b: Vec, t: Double): Vec {
        val u = 1.0 - t
        return Vec(
            u * u * a.x + 2.0 * u * t * control.x + t * t * b.x,
            u * u * a.y + 2.0 * u * t * control.y + t * t * b.y,
        )
    }

    fun sample(a: Vec, b: Vec, steps: Int = 18): List<Vec> {
        val control = controlPoint(a, b)
        return (0..steps).map { i -> pointOnQuad(a, control, b, i / steps.toDouble()) }
    }

    fun distanceToSegment(p: Vec, a: Vec, b: Vec): Double {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val len2 = dx * dx + dy * dy
        if (len2 < 0.001) return hypot(p.x - a.x, p.y - a.y)
        val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / len2).coerceIn(0.0, 1.0)
        val px = a.x + t * dx
        val py = a.y + t * dy
        return hypot(p.x - px, p.y - py)
    }

    fun distanceToPolyline(p: Vec, points: List<Vec>): Double {
        if (points.size < 2) return Double.MAX_VALUE
        var best = Double.MAX_VALUE
        for (i in 0 until points.lastIndex) {
            val d = distanceToSegment(p, points[i], points[i + 1])
            if (d < best) best = d
        }
        return best
    }

    fun nearestHit(p: Vec, polylines: List<Pair<String, List<Vec>>>, slop: Double = HIT_SLOP): String? {
        var bestId: String? = null
        var best = slop
        for ((id, line) in polylines) {
            val d = distanceToPolyline(p, line)
            if (d <= best) {
                best = d
                bestId = id
            }
        }
        return bestId
    }
}
