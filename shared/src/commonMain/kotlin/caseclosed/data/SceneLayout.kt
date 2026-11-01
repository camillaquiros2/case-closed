package caseclosed.data

import caseclosed.model.PercentBox
import caseclosed.model.SceneItemSpot

internal fun box(x: Double, y: Double, w: Double, h: Double) = PercentBox(x, y, w, h)

internal fun spot(itemId: String, x: Double, y: Double, w: Double, h: Double) =
    SceneItemSpot(itemId, PercentBox(x, y, w, h))
