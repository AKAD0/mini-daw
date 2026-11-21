// Module :app: the activity, the order of startup, the wiring of all parts (architecture.md Section 2.1)
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// The file of the key for signing lies in the repository under a password (D-094). The CI service passes
// the password in MINIDAW_SIGNING_PASSWORD. Without both, the release build and the test build stay unsigned.
val signingStore = rootProject.file("signing/minidaw.p12")
val signingPassword = providers.environmentVariable("MINIDAW_SIGNING_PASSWORD").orNull?.takeIf { it.isNotBlank() }
val buildNumber = providers.environmentVariable("MINIDAW_VERSION_CODE").orNull?.toIntOrNull() ?: 1
val changeId = providers.environmentVariable("GITHUB_SHA").orNull?.take(12) ?: "local"

android {
    namespace = "app.minidaw"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        applicationId = "app.minidaw"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = buildNumber
        versionName = "0.1.$buildNumber"
        buildConfigField("String", "CHANGE_ID", "\"$changeId\"")
        buildConfigField("boolean", "TEST_BUILD", "false")
    }
    buildFeatures {
        buildConfig = true
    }
    signingConfigs {
        if (signingStore.exists() && signingPassword != null) {
            create("minidaw") {
                storeFile = signingStore
                storePassword = signingPassword
                keyAlias = "minidaw"
                keyPassword = signingPassword
                storeType = "pkcs12"
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("minidaw")
            // Release builds hold 64-bit ARM only (D-034). The debug build also holds x86_64.
            ndk {
                abiFilters += listOf("arm64-v8a")
            }
        }
        // The test build (development_roadmap.md Section 2.2): optimized like the release build, with the
        // screen "Measurements", its own application ID and its own name (D-098).
        create("measure") {
            initWith(getByName("release"))
            applicationIdSuffix = ".test"
            matchingFallbacks += listOf("release")
            buildConfigField("boolean", "TEST_BUILD", "true")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":engine-api"))
    implementation(project(":core"))
    implementation(project(":engine"))
    implementation(project(":analysis"))
    implementation(project(":data"))
    implementation(project(":extraction"))
    implementation(project(":ui:kit"))
    implementation(project(":ui:workspace"))
    implementation(project(":ui:editors"))
    implementation(project(":ui:mixer"))
    implementation(project(":ui:library"))
    implementation(project(":ui:decomposer"))
}
