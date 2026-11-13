package caseclosed.ui

import korlibs.korge.view.Container
import korlibs.korge.view.position
import korlibs.korge.view.text

/**
 * Tutorial opened from the main menu.
 * R15 fills this screen; for now it only shows that it is coming and lets the player go back.
 */
fun buildTutorial(root: Container, nav: Navigator) {
    root.woodBackground(nav.assets)
    root.chrome(GameText.MENU_TUTORIAL, GameText.TITLE, nav, back = true)
    root.text(GameText.TUTORIAL_SOON, textSize = 22.0, color = Palette.paper) {
        position(32.0, Desk.HEADER + 48.0)
    }
}
