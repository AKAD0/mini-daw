// Modules of mini daw (architecture.md Section 2.1, D-113)
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
