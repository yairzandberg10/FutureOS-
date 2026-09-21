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
        // sherpa-onnx (Piper TTS) מגיע כ-AAR מוכן מ-GitHub Releases, לא ממאגר
        // Maven - נטען כקובץ מקומי מ-app/libs.
        flatDir { dirs("app/libs") }
    }
}
rootProject.name = "Assistant"
include(":app")

// מודול קיט הפוקוס/T9/עיצוב המשותף לסוויטה - חי כתיקייה אחות עצמאית
// (../SharedKeypadNav), לא submodule של Assistant, כדי ש-Assistant יישאר
// build/פרויקט אנדרואיד-סטודיו עצמאי לגמרי כמו שהיה.
include(":sharedkeypadnav")
project(":sharedkeypadnav").projectDir = File(rootDir, "../SharedKeypadNav/sharedkeypadnav")
