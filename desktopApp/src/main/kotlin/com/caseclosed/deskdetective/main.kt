package com.caseclosed.deskdetective

import kotlin.system.exitProcess

suspend fun main() = launchCaseClosedGame(onExit = { exitProcess(0) })
