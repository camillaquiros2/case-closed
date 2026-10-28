import korlibs.korge.gradle.*

plugins {
    id("com.soywiz.korge.library")
}

korge {
    id = "com.caseclosed.deskdetective.shared"
    // Library manifest only. The launcher activity lives in :androidApp.
    androidLibrary = true

    targetJvm()
    targetJs()
    targetWasm()
    targetIos()
    targetAndroid()

    serializationJson()
}

dependencies {
    add("commonMainApi", project(":deps"))
}
