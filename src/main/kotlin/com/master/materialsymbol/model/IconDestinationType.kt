package com.master.materialsymbol.model

enum class IconDestinationType(val displayName: String) {
    COMPOSE("Jetpack Compose (ImageVector)"),
    XML("Android XML (Drawable)"),
    BOTH("Both (Compose + XML)");

    val isCompose: Boolean get() = this == COMPOSE || this == BOTH
    val isXml: Boolean get() = this == XML || this == BOTH
}
