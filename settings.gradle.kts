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
    }
}

rootProject.name = "LiveTranslate-Pro"

include(
    ":app",
    ":core:model",
    ":core:common",
    ":core:database",
    ":core:ui",
    ":domain",
    ":data:auth",
    ":data:translation",
    ":feature:auth",
    ":feature:home",
    ":feature:translate",
    ":feature:history",
    ":feature:settings",
)
