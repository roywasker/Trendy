import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.serialization)
}

kotlin {
    android {
        namespace = "org.roy.checkout"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources { enable = true }
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "checkout"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.ktor.android.client)
        }
        iosMain.dependencies {
            implementation(libs.ktor.darwin.client)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)

            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.messagebar.kmp)

            implementation(libs.firebase.app)
            implementation(libs.firebase.auth)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization)
            implementation(libs.browser.kmp)

            implementation(project(path = ":shared"))
            implementation(project(path = ":data"))
        }
    }
}
