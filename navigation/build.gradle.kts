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
        namespace = "org.roy.navigation"
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
            baseName = "navigation"
            isStatic = true
        }
    }

    sourceSets {

        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.kotlinx.serialization)
            implementation(libs.compose.navigation)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)

            implementation(project(path = ":feature:auth"))
            implementation(project(path = ":feature:home"))
            implementation(project(path = ":feature:profile"))
            implementation(project(path = ":feature:admin_panel"))
            implementation(project(path = ":feature:details"))
            implementation(project(path = ":feature:admin_panel:manage_product"))
            implementation(project(path = ":feature:home:categories:category_search"))
            implementation(project(path = ":feature:home:cart:checkout"))
            implementation(project(path = ":feature:payment_completed"))
            implementation(project(path = ":shared"))
        }
    }
}
