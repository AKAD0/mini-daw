// Modules of mini daw (architecture.md Section 2.1, D-113)
// Google's mirror of Maven Central stands after Maven Central. Three runs "Check" got a false "not
// found" from Maven Central for files that exist; Gradle then asks the mirror (TK-004).
pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
    }
}

rootProject.name = "mini-daw"

include(":engine-api")
include(":core")
include(":engine")
include(":analysis")
include(":data")
include(":extraction")
include(":ui:kit")
include(":ui:workspace")
include(":ui:editors")
include(":ui:mixer")
include(":ui:library")
include(":ui:decomposer")
include(":app")
include(":benchmark")

// Apps for the check V-02 of PH-00. They are no part of the product (architecture.md Section 2.1).
include(":checkapps:views")
include(":checkapps:compose")
// Both pitch candidates of the check V-05, for the test build only (TK-010)
include(":checkapps:humming")
