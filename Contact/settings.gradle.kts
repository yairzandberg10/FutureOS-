pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Contact"
include(":app")

// מודול קיט הפוקוס/T9 המשותף לסוויטה - חי כתיקייה אחות עצמאית
// (../SharedKeypadNav), לא submodule של Contact, כדי ש-Contact יישאר
// build/פרויקט אנדרואיד-סטודיו עצמאי לגמרי כמו שהיה (כמו ב-dialer).
include(":sharedkeypadnav")
project(":sharedkeypadnav").projectDir = File(rootDir, "../SharedKeypadNav/sharedkeypadnav")
 