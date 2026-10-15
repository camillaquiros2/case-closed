package caseclosed.ui

import caseclosed.engine.GameEngine
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.addUpdater
import korlibs.korge.view.position
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import korlibs.time.DateTime

sealed class Route {
    data object Title : Route()
    data object Desk : Route()
    data object Phone : Route()
    data object Files : Route()
    data class Document(val fileId: String) : Route()
    data object Suspects : Route()
    data class Interview(val suspectId: String) : Route()
    data object Items : Route()
    data class ItemDetail(val itemId: String) : Route()
    data object Board : Route()
    data object Conclusion : Route()
    data object SceneList : Route()
    data class SceneMap(val sceneId: String) : Route()
    data class SceneCloseup(val sceneId: String, val subSceneId: String) : Route()
    data class SceneFind(val sceneId: String, val subSceneId: String, val itemId: String) : Route()
}

class Navigator(
    private val root: Container,
    val engine: GameEngine,
    val assets: AssetBank,
) {
    private val stack = ArrayDeque<Route>()
    private var layer: Container? = null
    var banner: String? = null

    fun canPop(): Boolean = stack.size > 1

    fun start(route: Route) {
        stack.clear()
        stack.addLast(route)
        show()
    }

    fun go(route: Route) {
        stack.clear()
        stack.addLast(route)
        show()
    }

    fun push(route: Route) {
        stack.addLast(route)
        show()
    }

    fun pop() {
        if (stack.size > 1) stack.removeLast()
        show()
    }

    fun refresh() = show()

    private fun showUnlockedQuestionNotice(host: Container) {
        val notices = mutableListOf<String>()
        while (true) {
            val message = engine.takeUnlockedQuestionNotice()
                ?: break

            notices += message
        }
        if (notices.isEmpty()) return
        val indicator = host.container {
            position(24.0, Desk.HEADER + 8.0)
            mouseEnabled = false
        }
        var index = 0
        var expiresAt = 0L

        fun displayNotice() {
            indicator.clearChildren()
            val message = notices[index].substringBefore("\n")
            val isItem = message.startsWith("Objeto descubierto:")
            val title = if (isItem) {
                "¡Objeto descubierto!"
            } else {
                "¡Nueva pregunta disponible!"
            }
            val detail = if (isItem) {
                message.removePrefix("Objeto descubierto:").trim()
            } else {
                message
            }
            val width = Desk.W - 48.0
            val textWidth = width - 32.0
            val textSize = 18.0
            val lines = wrapLines(
                detail,
                textWidth,
                textSize,
            )
            val height = 64.0 +
                wordWrapHeight(lines.size, textSize)
            indicator.solidRect(
                width,
                height,
                Palette.gold,
            )
            indicator.solidRect(
                width,
                4.0,
                Palette.wax,
            )
            indicator.text(
                title,
                textSize = 20.0,
                color = Palette.ink,
            ) {
                position(16.0, 14.0)
            }
            indicator.blockText(
                detail,
                16.0,
                46.0,
                textWidth,
                textSize,
                Palette.ink,
            )
            expiresAt = DateTime.now().unixMillisLong + 5_000L
        }
        displayNotice()
        indicator.addUpdater {
            if (DateTime.now().unixMillisLong >= expiresAt) {
                index += 1

                if (index < notices.size) {
                    displayNotice()
                } else {
                    indicator.removeFromParent()
                }
            }
        }
    }

    private fun show() {
        layer?.removeFromParent()
        val next = root.container()
        layer = next
        when (val route = stack.last()) {
            Route.Title -> buildTitle(next, this)
            Route.Desk -> buildDesk(next, this)
            Route.Phone -> buildPhone(next, this)
            Route.Files -> buildFiles(next, this)
            is Route.Document -> buildDocument(next, this, route.fileId)
            Route.Suspects -> buildSuspects(next, this)
            is Route.Interview -> buildInterview(next, this, route.suspectId)
            Route.Items -> buildItems(next, this)
            is Route.ItemDetail -> buildItemDetail(next, this, route.itemId)
            Route.Board -> buildBoard(next, this)
            Route.Conclusion -> buildConclusion(next, this)
            Route.SceneList -> buildSceneList(next, this)
            is Route.SceneMap -> buildSceneMap(next, this, route.sceneId)
            is Route.SceneCloseup -> buildSceneCloseup(next, this, route.sceneId, route.subSceneId)
            is Route.SceneFind -> buildSceneFind(next, this, route.sceneId, route.subSceneId, route.itemId)
        }
        showUnlockedQuestionNotice(next)
    }
}
