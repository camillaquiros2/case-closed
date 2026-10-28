import korlibs.korge.gradle.*

plugins {
    alias(libs.plugins.korge)
}

korge {
    id = "com.caseclosed.deskdetective"
    name = "Detective de mesa"
    targetDesktop()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            resources.srcDir(rootProject.file("public/assets"))
        }
    }
}

dependencies {
    add("commonMainApi", project(":shared"))
}
