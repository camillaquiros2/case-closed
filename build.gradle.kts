import korlibs.korge.gradle.*

plugins {
	alias(libs.plugins.korge)
}

korge {
	id = "com.caseclosed.deskdetective"

// To enable all targets at once

	//targetAll()

// To enable targets based on properties/environment variables
	//targetDefault()

// To selectively enable targets
	
	targetJvm()
	targetJs()
    targetWasm()
	targetDesktop()
	targetIos()
	targetAndroid()

	serializationJson()
}


kotlin {
    sourceSets {
        val commonMain by getting {
            // Drop-in art folder. Paths are images/desk/... and images/cases/...
            resources.srcDir("public/assets")
        }
    }
}

dependencies {
    add("commonMainApi", project(":deps"))
    //add("commonMainApi", project(":korge-dragonbones"))
}

