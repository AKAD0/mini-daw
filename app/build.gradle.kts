// Module :app: the activity, the order of startup, the wiring of all parts (architecture.md Section 2.1)
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "app.minidaw"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        applicationId = "app.minidaw"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
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
