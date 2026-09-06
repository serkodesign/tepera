pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Tepera"
include(":app")

// Свідомо один модуль — мультимодульність поза обсягом MVP (див. CLAUDE.md)
