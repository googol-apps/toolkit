// -----------------------------------------------------------------------------
// PLUGINS
// -----------------------------------------------------------------------------
// 📦 Core plugins required for Android Library + Compose + Maven publishing.
plugins {
    alias(libs.plugins.android.library)       // Android library plugin
    alias(libs.plugins.compose.compiler)      // Jetpack Compose compiler plugin
    id(libs.plugins.maven.publish.get().pluginId) // Maven publishing plugin
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
            "-Xnon-local-break-continue",    // Allow non-local break/continue
            "-Xcontext-sensitive-resolution",// Context-sensitive overload resolution
            "-Xcontext-parameters"           // Enable context parameters (experimental)
        )
    }
}


// -----------------------------------------------------------------------------
// ANDROID CONFIGURATION
// -----------------------------------------------------------------------------
// 🎨 Android library setup for Compose theme utilities.
android {
    namespace = "com.zs.compose.theme"
    compileSdk { version = release(37) { minorApiLevel = 2 } }

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Java 17 compatibility
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Enable Jetpack Compose
    buildFeatures { compose = true }

    // Configure publishing artifacts (sources + Javadoc)
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}


// -----------------------------------------------------------------------------
// DEPENDENCIES
// -----------------------------------------------------------------------------
// 📚 Core libraries: Compose foundation, ripple effects, window APIs, and project modules.
dependencies {
    implementation(platform(libs.compose.bom))        // Compose BOM for version alignment
    implementation(project(":foundation"))            // Internal foundation module
    implementation(libs.androidx.foundation)          // Compose foundation components
    implementation(libs.androidx.material.ripple)     // Ripple effect for touch feedback
    implementation(libs.androidx.window)              // WindowManager APIs (foldables, multi-window)
}


// -----------------------------------------------------------------------------
// PUBLISHING CONFIGURATION
// -----------------------------------------------------------------------------
// 📤 Configure Maven publication after evaluation phase.
afterEvaluate {
    publishing {
        publications {
            // Create a Maven publication named "release"
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.googol-apps"
                artifactId = "theme"
                version = "3.12.2-dev"
            }
        }
    }
}