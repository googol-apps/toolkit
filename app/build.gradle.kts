import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// -----------------------------------------------------------------------------
// PLUGINS
// -----------------------------------------------------------------------------
// 📦 Core plugins required for Android + Kotlin + Compose support.
plugins {
    alias(libs.plugins.android.application)   // Android application plugin
    alias(libs.plugins.compose.compiler)      // Jetpack Compose compiler plugin
}


// -----------------------------------------------------------------------------
// KOTLIN COMPILER OPTIONS
// -----------------------------------------------------------------------------
kotlin {
    compilerOptions {

        // Add experimental/advanced compiler flags
        freeCompilerArgs.addAll(
            //   "-XXLanguage:+ExplicitBackingFields", //  Explicit backing fields
            "-XXLanguage:+NestedTypeAliases",
            "-Xopt-in=kotlin.RequiresOptIn", // Opt-in to @RequiresOptIn APIs
            "-Xwhen-guards",                 // Enable experimental when-guards
            "-Xopt-in=androidx.compose.foundation.ExperimentalFoundationApi", // Compose foundation experimental
            "-Xopt-in=com.zs.compose.theme.ExperimentalThemeApi",             // Custom theme experimental
            "-Xnon-local-break-continue",    // Allow non-local break/continue
            "-Xcontext-sensitive-resolution",// Context-sensitive overload resolution
            "-Xcontext-parameters"           // Enable context parameters (experimental)
        )
    }
}

// -----------------------------------------------------------------------------
// ANDROID CONFIGURATION
// -----------------------------------------------------------------------------
// 📱 Android project setup: namespace, SDK levels, build types, and features.
android {
    namespace = "com.prime.toolkit"
    compileSdk = 37
    buildFeatures { compose = true }  // Enable Jetpack Compose
    // Java 17 compatibility
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // -----------------------------------------------------------------------------
    // DEFAULT CONFIGURATION
    // -----------------------------------------------------------------------------
    // 📦 Core app settings: ID, SDK versions, versioning, and test runner.
    defaultConfig {
        applicationId = "com.prime.toolkit"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "3.0.0-dev"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true } // Support library for vector drawables
    }

    // -----------------------------------------------------------------------------
    // Build Types
    // -----------------------------------------------------------------------------
    buildTypes {
        // -------------------------------------------------------------------------
        // RELEASE BUILD
        // -------------------------------------------------------------------------
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }

}


// -----------------------------------------------------------------------------
// DEPENDENCIES
// -----------------------------------------------------------------------------
// 📚 Core libraries: Compose, Material, Coil, and project modules.
dependencies {
    // Import Compose BOM (aligns versions across Compose artifacts)
    implementation(platform(libs.compose.bom))

    // Jetpack Compose + AndroidX
    implementation(libs.androidx.activity.compose)       // Activity integration
    implementation(libs.androidx.material3)              // Material Design 3 components
    implementation(libs.androidx.material.icons.core)    // Core Material icons
    implementation(libs.androidx.material.icons.extended)// Extended Material icons
    implementation(libs.androidx.ui.tooling)             // Debugging + preview tooling
    implementation(libs.androidx.ui.tooling.preview)     // Preview annotations

    // Image loading
    implementation(libs.coil.compose)                    // Coil integration for Compose
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.6.2") // Coil with OkHttp backend

    // Project modules
    implementation(project(":theme"))                    // Custom theme module
    implementation(project(":foundation"))               // Foundation utilities
    implementation(project(":preferences"))              // Preferences module

    // Experimental / third-party
    implementation(libs.chrisbanes.haze)                 // Blur + haze effects
}