package com.jarvis.app.navigation

sealed class JarvisRoute(val path: String) {
    data object Main : JarvisRoute("main")
    data object Settings : JarvisRoute("settings")
}
