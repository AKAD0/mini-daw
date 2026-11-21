// Root of the build. The modules apply the plugins (architecture.md Section 2.1).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// Static analysis of Kotlin (technology_stack.md Section 12). Both tools run as programs of their own,
// so their versions do not depend on the version of Kotlin in the modules.
val ktlintCli: Configuration by configurations.creating
val detektCli: Configuration by configurations.creating

dependencies {
    ktlintCli(libs.ktlint.cli) {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
    detektCli(libs.detekt.cli)
}

tasks.register<JavaExec>("ktlint") {
    group = "verification"
    description = "Checks the style of all Kotlin files with ktlint."
    classpath = ktlintCli
    mainClass.set("com.pinterest.ktlint.Main")
    args("**/src/**/*.kt", "**/*.kts", "!**/build/**")
}

tasks.register<JavaExec>("detekt") {
    group = "verification"
    description = "Runs detekt on the Kotlin sources of all modules."
    classpath = detektCli
    mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
    args("--input", rootDir.absolutePath)
    args("--excludes", "**/build/**,**/*.kts")
    args("--config", file("config/detekt.yml").absolutePath)
    args("--build-upon-default-config")
}
