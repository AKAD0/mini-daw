// Empty app with Compose and the prototype of the workspace for the check V-02 (TK-004)
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// The key for signing of all builds (development_roadmap.md Section 2.2, D-094)
val signingStore = rootProject.file("signing/minidaw.p12")
val signingPassword = providers.environmentVariable("MINIDAW_SIGNING_PASSWORD").orNull?.takeIf { it.isNotBlank() }
val buildNumber = providers.environmentVariable("MINIDAW_VERSION_CODE").orNull?.toIntOrNull() ?: 1

android {
    namespace = "app.minidaw.check.compose"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        applicationId = "app.minidaw.check.compose"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = buildNumber
        versionName = "0.1.$buildNumber"
        // 64-bit ARM only, as the release build of the product (D-034)
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }
    buildFeatures {
        compose = true
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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("minidaw")
        }
    }
    sourceSets {
        getByName("main") {
            java.srcDir("../shared/kotlin")
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
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.activity.compose)
    implementation(libs.profileinstaller)
}
