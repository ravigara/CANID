pluginManagement {
    repositories {
        // Try the plugin portal first, then Google and Maven Central
        gradlePluginPortal()
        google()
        mavenCentral()
    }

    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "com.android.application" ->
                    useModule("com.android.tools.build:gradle:8.4.2")
                "org.jetbrains.kotlin.android" ->
                    useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24")
                "com.google.devtools.ksp" ->
                    useModule("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:1.9.24-1.0.20")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}

rootProject.name = "CANID"
include(":app")
