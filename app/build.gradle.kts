// Import Gradle DSL helpers for Android and Kotlin configuration
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.android.build.api.dsl.ApplicationDefaultConfig as Config

// -----------------------------
// Plugins Section
// -----------------------------
plugins {
    alias(libs.plugins.android.application)  // Android Application plugin → marks this module as the main app entry point
    alias(libs.plugins.kotlin.compose) // Kotlin Compose plugin → adds Jetpack Compose compiler integration
}

///
//  -------------------------------------------------------------------------------------
//  APPLICATION CONFIGURATION
//  -------------------------------------------------------------------------------------
//  Defines the core identity and compatibility parameters of the application.
//  This lambda is applied to the [defaultConfig] block within the [android] section.
private val config: Config.() -> Unit = {
    versionCode = 100                                    // Internal version code
    versionName = "4.0.0"                               // User-facing version name
    applicationId = "com.zs.toolkit" // Unique app ID
    minSdk = 24                                         // Minimum supported Android version
    targetSdk = 37                                      // Target SDK
}

// -----------------------------
// Kotlin Compiler Options
// -----------------------------
// Configure Kotlin compiler with JVM target and advanced language features
kotlin {
    compilerOptions {
        // Target JVM bytecode version (modern Java 17 features)
        jvmTarget = JvmTarget.JVM_17

        // Add experimental/advanced compiler flags
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",        // Opt-in to @RequiresOptIn APIs
            "-XXLanguage:+NestedTypeAliases",
            "-Xwhen-guards",                        // Enable experimental when-guards
            "-Xcompanion-blocks-and-extensions"     // Enable companion fun and block
        )
    }
}

// -----------------------------
// Compose Compiler Configuration
// -----------------------------
composeCompiler {
    // Stability configuration for Compose compiler
    // TODO - I guess disable these in release builds.reportsDestination =
    //     layout.buildDirectory.dir("compose_compiler")
    // metricsDestination = layout.buildDirectory.dir("compose_compiler")
    stabilityConfigurationFiles = listOf(
        rootProject.layout.projectDirectory.file("stability_config.conf")
    )
}

// -----------------------------
// Android Configuration
// -----------------------------
android {
    buildFeatures { compose = true } // Enable Compose
    namespace = "com.zs.toolkit" // Unique namespace for the app
    compileSdk { version = release(37) { minorApiLevel = 2 } } // Compile against Android SDK level 36 → latest APIs available
    defaultConfig(config)
    packaging { // Exclude redundant license files from packaging
        jniLibs.keepDebugSymbols.add("**/*.so")
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }

    // -----------------------------
    // Java Compatibility Options
    // -----------------------------
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // -----------------------------------------------------------------------------
    // Build Types
    // -----------------------------------------------------------------------------
    buildTypes {
        // -------------------------------------------------------------------------
        // RELEASE BUILD
        // -------------------------------------------------------------------------
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
}

// -----------------------------------------------------------------------------
// DEPENDENCIES
// -----------------------------------------------------------------------------
// 📚 Core libraries: Compose, Material, Coil, and project modules.
dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Project modules
    implementation(project(":theme"))                    // Custom theme module
    implementation(project(":foundation"))               // Foundation utilities
    implementation(project(":preferences"))              // Preferences module
}