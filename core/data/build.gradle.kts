@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.android.build.gradle.internal.tasks.CompileArtProfileTask
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

val isFullBuild: Boolean =
    try {
        extra["isFullBuild"] == "true"
    } catch (e: Exception) {
        false
    }

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xmulti-dollar-interpolation")
        freeCompilerArgs.add("-Xwhen-guards")
    }
    // Target declarations - add or remove as needed below. These define
    // which platforms this KMP module supports.
    // See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
    android {
        namespace = "com.maxrave.data"
        compileSdk = 37
        minSdk = 26
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }

    dependencies {
        implementation(platform(libs.koin.bom))
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.common)
                implementation(projects.domain)
                implementation(projects.autoEqService)
                implementation(projects.lyricsService)
                implementation(projects.kotlinYtmusicScraper)
                implementation(projects.discord)
                implementation(projects.listenTogether)

                // Last.fm (gated: real scrobbler for full builds, no-op stub for FOSS builds)
                if (isFullBuild) {
                    implementation(projects.lastfm)
                } else {
                    implementation(projects.lastfmEmpty)
                }

                implementation(libs.kotlin.stdlib)
                // Add KMP dependencies here
                // Kotlinx serialization
                implementation(libs.kotlinx.serialization.json)

                // DataStore
                implementation(libs.datastore.preferences)

                // Room
                implementation(libs.room.runtime)
                implementation(libs.androidx.sqlite.bundled)
                implementation(libs.androidx.room.migration)

                // Koin
                implementation(libs.koin.core)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                // Add Android-specific dependencies here. Note that this source set depends on
                // commonMain by default and will correctly pull the Android artifacts of any KMP
                // dependencies declared in commonMain.
                implementation(libs.koin.android)
                implementation(projects.media3)
                implementation(libs.room.ktx)
            }
        }

    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
}

tasks.withType<CompileArtProfileTask> {
    enabled = false
}