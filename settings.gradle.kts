pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()

        jcenter()
        maven { url =uri("https://jitpack.io") }
        // maven { url 'https://maven.fabric.io/public' }

    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        jcenter()
        maven { url =uri("https://jitpack.io") }
    }
}

rootProject.name = "ExoDemo"
include(":app")
 