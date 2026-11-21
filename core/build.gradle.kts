// Module :core: state of a Project, Edits, Reducer, undo, compiler to a Snapshot (architecture.md Section 2.1)
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    api(project(":engine-api"))
    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlin.test)
    testImplementation(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.platform.launcher)
}

// The chain of the fixtures (architecture.md Section 15, TK-020): every Project of fixtures/projects becomes a
// Snapshot in build/fixtures. The tests check each one; the tests of engine-core load them (PH-02).
val fixtures = rootProject.layout.projectDirectory.dir("fixtures/projects")
val snapshots = layout.buildDirectory.dir("fixtures")
val compileFixtures by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Compiles every fixture of fixtures/projects into a Snapshot."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("app.minidaw.core.compiler.FixtureCompilerKt")
    inputs.dir(fixtures)
    outputs.dir(snapshots)
    args(fixtures.asFile.path, snapshots.get().asFile.path)
}

tasks.test {
    dependsOn(compileFixtures)
    systemProperty("minidaw.fixtures", fixtures.asFile.path)
    systemProperty("minidaw.snapshots", snapshots.get().asFile.path)
    useJUnitPlatform()
    // Each test names its outcome in the log of the run "Check".
    testLogging {
        events("passed", "skipped", "failed")
    }
}
