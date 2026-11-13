package com.caseclosed.deskdetective

import caseclosed.data.CaseCatalog
import caseclosed.dev.SceneGuides
import caseclosed.engine.GameEngine
import caseclosed.persist.NativeGameSaves
import caseclosed.ui.AssetBank
import caseclosed.ui.Desk
import caseclosed.ui.GameText
import caseclosed.ui.Navigator
import caseclosed.ui.Route
import korlibs.image.color.Colors
import korlibs.korge.Korge
import korlibs.korge.scene.Scene
import korlibs.korge.scene.sceneContainer
import korlibs.korge.service.storage.storage
import korlibs.korge.view.SContainer
import korlibs.korge.view.solidRect
import korlibs.math.geom.ScaleMode
import korlibs.math.geom.Size
import korlibs.time.DateTime

/** [onExit] closes the app on this platform. Leave it null where the game can't close itself (web). */
suspend fun launchCaseClosedGame(onExit: (() -> Unit)? = null) {
    Korge(
        title = GameText.WINDOW_TITLE,
        windowSize = Size(405, 720),
        virtualSize = Size(Desk.W, Desk.H),
        backgroundColor = Colors["#140D0A"],
        scaleMode = ScaleMode.SHOW_ALL,
    ) {
        val sceneContainer = sceneContainer()
        sceneContainer.changeTo { GameScene(onExit) }
    }
}

class GameScene(private val onExit: (() -> Unit)? = null) : Scene() {
    override suspend fun SContainer.sceneMain() {
        CaseCatalog.validate()
        SceneGuides.writeIfEnabled()
        solidRect(Desk.W, Desk.H, Colors["#140D0A"])
        val assets = AssetBank()
        assets.preload(CaseCatalog.imagePaths())
        val engine = GameEngine(NativeGameSaves(views.storage), now = { DateTime.now().unixMillisLong })
        engine.load()
        Navigator(this, engine, assets, onExit).start(Route.Title)
    }
}
