pluginManagement {
    repositories {
        // Repositories used to resolve Gradle plugins
        google()                     // Needed for Android Gradle Plugin & CameraX
        mavenCentral()               // Kotlin & other libraries
        gradlePluginPortal()         // For Gradle plugins
    }
}

// settings.gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "usul"
include(":app")
