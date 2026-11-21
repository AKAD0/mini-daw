// Module :checkapps:humming: both pitch candidates of the check V-05 for the test build (TK-010).
// It is no part of the product: only the build type "measure" of :app uses it. It compiles
// analysis-core and SwiftF0 against ONNX Runtime, which the official AAR brings.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

// The AAR of ONNX Runtime holds the headers and the libraries, but no prefab package. A task
// takes both out of it for CMake.
val onnxRuntimeAar: Configuration by configurations.creating {
    isTransitive = false
}
val onnxRuntimeDir = layout.buildDirectory.dir("onnxruntime")
val extractOnnxRuntime by tasks.registering(Sync::class) {
    from({ zipTree(onnxRuntimeAar.singleFile) }) {
        include("headers/**", "jni/arm64-v8a/libonnxruntime.so", "jni/x86_64/libonnxruntime.so")
    }
    into(onnxRuntimeDir)
}

android {
    namespace = "app.minidaw.check.humming"
    compileSdk = libs.versions.compileSdk.get().toInt()
    ndkVersion = libs.versions.ndk.get()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
        externalNativeBuild {
            cmake {
                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DMINIDAW_ORT_DIR=" + onnxRuntimeDir.get().asFile.absolutePath
                )
            }
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = libs.versions.cmake.get()
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

tasks.matching { it.name.startsWith("configureCMake") || it.name.startsWith("buildCMake") }.configureEach {
    dependsOn(extractOnnxRuntime)
}

dependencies {
    onnxRuntimeAar(libs.onnxruntime.android) {
        artifact {
            type = "aar"
        }
    }
    // Packs libonnxruntime.so into the test build.
    implementation(libs.onnxruntime.android)
}
