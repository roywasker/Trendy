import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.google.services)
}

android {
    namespace = "org.roy.ecommerce_project"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.roy.ecommerce_project"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "org.roy.ecommerce_project.testing.TrendyTestRunner"
        testInstrumentationRunnerArguments["class"] = "org.roy.ecommerce_project.testing.AllFeaturesTestSuite"
    }
    buildFeatures { compose = true }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
    buildTypes {
        getByName("release") { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(project(":shared"))
    implementation(project(":di"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.splash.screen)
    implementation(libs.koin.android)
    implementation(libs.firebase.app)
    implementation(libs.kmp.notifier)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.compose.ui.test)
    androidTestImplementation(libs.compose.material3)
    androidTestImplementation(libs.compose.foundation)
    androidTestImplementation(libs.auth.firebase.kmp)
    androidTestImplementation(libs.ktor.client.mock)
    androidTestImplementation(libs.ktor.client.core)
    androidTestImplementation(libs.ktor.client.content.negotiation)
    androidTestImplementation(libs.ktor.client.serialization)
    androidTestImplementation(libs.kotlinx.serialization)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.multiplatform.settings.test)
    androidTestImplementation(libs.multiplatform.settings)
    androidTestImplementation(libs.multiplatform.settings.make.observable)
    androidTestImplementation(libs.firebase.storage)
    androidTestImplementation(libs.koin.compose)
    androidTestImplementation(libs.koin.compose.viewmodel)
    androidTestImplementation(libs.androidx.lifecycle.viewmodel)
    androidTestImplementation(project(":data"))
    androidTestImplementation(project(":navigation"))
    androidTestImplementation(project(":feature:auth"))
    androidTestImplementation(project(":feature:home"))
    androidTestImplementation(project(":feature:home:products_overview"))
    androidTestImplementation(project(":feature:home:categories"))
    androidTestImplementation(project(":feature:home:categories:category_search"))
    androidTestImplementation(project(":feature:home:cart"))
    androidTestImplementation(project(":feature:home:cart:checkout"))
    androidTestImplementation(project(":feature:details"))
    androidTestImplementation(project(":feature:profile"))
    androidTestImplementation(project(":feature:admin_panel"))
    androidTestImplementation(project(":feature:admin_panel:manage_product"))
    androidTestImplementation(project(":feature:payment_completed"))
}

// A disconnected device must not produce a green build with an empty test report.
tasks.matching { it.name == "connectedDebugAndroidTest" }.configureEach {
    val resultsDirectory = layout.buildDirectory.dir("outputs/androidTest-results/connected/debug")
    doLast {
        val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        val executed = resultsDirectory.get().asFile.walkTopDown()
            .filter { it.isFile && it.name.startsWith("TEST-") && it.extension == "xml" }
            .sumOf { file -> factory.newDocumentBuilder().parse(file).documentElement.getAttribute("tests").toIntOrNull() ?: 0 }
        check(executed > 0) { "No instrumented tests ran. Check the device connection and test filter; this run is not a pass." }
    }
}
