package caseclosed.ui

import caseclosed.engine.GameEngine
import korlibs.korge.view.Container
import korlibs.korge.view.container

sealed class Route {
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
        val message = engine.takeUnlockedQuestionNotice()
            ?: return

        host.confirmDialog(
            "Nueva pregunta disponible",
            message,
            "Continuar",
        ) {
            showUnlockedQuestionNotice(host)
        }
    }

    private fun show() {
        layer?.removeFromParent()
        val next = root.container()
        layer = next
        when (val route = stack.last()) {
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
        }
        showUnlockedQuestionNotice(next)
    }
}
