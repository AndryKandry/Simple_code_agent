@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.reload.gradle.ComposeHotRun
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
    alias(libs.plugins.android.application)
    alias(libs.plugins.hotReload)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
}

kotlin {
    androidTarget {
        //https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-test.html
        instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
    }

    jvm()

//    wasmJs {
//        browser()
//        binaries.executable()
//    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.uuid)

            implementation(libs.koin.core)
            implementation(libs.koin.compose.kmp)
            implementation(libs.koin.compose.viewmodel.kmp)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)

            implementation(libs.composeImageLoader)
            implementation(libs.multiplatformSettings)

            implementation(libs.room.runtime)
            implementation(libs.room.sqlite)
            implementation(libs.room.sqlite.bundled)

            implementation(libs.compose.viewmodel)
            implementation(libs.compose.navigation)
            implementation(libs.compose.material.icons)
            implementation(libs.compose.ui.backhandler)

            implementation(libs.kermit.logging)

            implementation(libs.coil.multiplatform.compose)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(compose.uiTooling)
            implementation(libs.androidx.activityCompose)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.okhttp)

            // CLI dependencies
            implementation(libs.clikt.core)
            implementation(libs.mordant.core)
            implementation(libs.jline.reader)
            implementation(libs.jline.terminal)
            implementation(libs.jline.terminal.jna)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

    }
}

android {
    namespace = "ru.agent"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        targetSdk = 35

        applicationId = "ru.defkmp.androidApp"
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

//https://developer.android.com/develop/ui/compose/testing#setup
dependencies {
    androidTestImplementation(libs.androidx.uitest.junit4)
    debugImplementation(libs.androidx.uitest.testManifest)
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "SimpleCodeAgent"
            packageVersion = "1.0.0"

            linux {
                iconFile.set(project.file("desktopAppIcons/LinuxIcon.png"))
            }
            windows {
                iconFile.set(project.file("desktopAppIcons/WindowsIcon.ico"))
            }
            macOS {
                iconFile.set(project.file("desktopAppIcons/MacosIcon.icns"))
                bundleID = "ru.agent.cli"
            }
        }
    }
}

tasks.withType<ComposeHotRun>().configureEach {
    mainClass = "MainKt"
}

dependencies {
    add("kspCommonMainMetadata", libs.room.compiler)
    add("kspAndroid", libs.room.compiler)
    add("kspIosX64", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
}

// Workaround for Room KMP issue: Room generates actual in metadata/commonMain which conflicts with expect
// Delete the generated actual file from metadata/commonMain after KSP processing
tasks.matching { it.name.contains("ksp", ignoreCase = true) }.configureEach {
    doLast {
        val generatedFile = file("build/generated/ksp/metadata/commonMain/kotlin/ru/agent/core/database/AppDatabaseConstructor.kt")
        if (generatedFile.exists()) {
            generatedFile.delete()
            logger.lifecycle("Deleted Room-generated actual from metadata/commonMain to avoid conflict with expect")
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

// CLI run task with proper terminal support
tasks.register("runCli", JavaExec::class) {
    group = "application"
    description = "Run the CLI application with proper terminal support"

    classpath = kotlin.jvm().compilations["main"].output.allOutputs +
                kotlin.jvm().compilations["main"].runtimeDependencyFiles

    mainClass.set("MainKt")

    // Enable proper terminal support
    standardInput = System.`in`
    standardOutput = System.out
    errorOutput = System.err

    // Enable ANSI colors and proper terminal handling
    // Let JLine auto-detect terminal type for better Unicode support
    val baseArgs = mutableListOf<String>()

    // Workaround for Gradle daemon issue with stdin
    if (System.console() != null) {
        baseArgs.add("-Djava.io.tmpdir=${System.getProperty("java.io.tmpdir")}")
    }

    jvmArgs = baseArgs
}
