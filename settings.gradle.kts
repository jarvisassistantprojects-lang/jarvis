pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://alphacephei.com/maven/") }
    }
}

rootProject.name = "jarvis"

include(":app")
include(":core:domain")
include(":core:voice")
include(":core:llm")
include(":core:actions")
include(":platform:voice-android")
include(":platform:android-control")
include(":providers:local-llm")
include(":providers:remote-llm")
include(":data:settings")
include(":data:security")
include(":data:logging")
