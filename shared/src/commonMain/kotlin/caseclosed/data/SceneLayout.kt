package caseclosed.data

import caseclosed.model.PercentBox
import caseclosed.model.SceneItemSpot

internal fun box(x: Double, y: Double, w: Double, h: Double) = PercentBox(x, y, w, h)

/** Transparent item PNG for one spot on a close-up. */
fun sceneItemPng(caseId: String, sceneId: String, subSceneId: String, itemId: String) =
    "images/cases/$caseId/scenes/$sceneId/$subSceneId/$itemId.png"

internal fun spot(itemId: String, image: String, x: Double, y: Double, w: Double, h: Double) =
    SceneItemSpot(itemId, PercentBox(x, y, w, h), image)
