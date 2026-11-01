package com.caseclosed.deskdetective

import korlibs.render.KorgwActivity

open class CaseClosedActivity : KorgwActivity() {
    override suspend fun activityMain() {
        launchCaseClosedGame()
    }
}
