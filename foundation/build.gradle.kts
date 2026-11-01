import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
// ⚙️ Configure Kotlin compiler for JVM + advanced language features.
kotlin {
    compilerOptions {
        // Enable experimental + advanced compiler flags
        freeCompilerArgs.addAll(
            // "-XXLanguage:+ExplicitBackingFields", // Explicit backing fields (disabled for now)
            "-XXLanguage:+NestedTypeAliases",       // Nested type aliases support
            "-Xopt-in=kotlin.RequiresOptIn",        // Opt-in to @RequiresOptIn APIs
            "-Xwhen-guards",                        // Experimental when-guards
            "-Xopt-in=androidx.compose.foundation.ExperimentalFoundationApi", // Compose foundation experimental
            "-Xnon-local-break-continue",           // Allow non-local break/continue
            "-Xcontext-sensitive-resolution",       // Context-sensitive overload resolution
            "-Xcontext-parameters"                  // Context parameters (experimental)
        )
    }
}


// -----------------------------------------------------------------------------
// ANDROID CONFIGURATION
// -----------------------------------------------------------------------------
// 📱 Android library setup: namespace, SDK levels, build types, and publishing.
android {
    namespace = "com.zs.compose.foundation"
    compileSdk { version = release(37) { minorApiLevel = 2 } }
    buildFeatures { compose = true }  // Enable Jetpack Compose

    // Java 17 compatibility
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

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
// 📚 Core libraries: Compose foundation + AndroidX utilities.
dependencies {
    implementation(platform(libs.compose.bom))     // Compose BOM for version alignment
    implementation(libs.androidx.foundation)       // Compose foundation components
    implementation(libs.androidx.core.ktx)         // Kotlin extensions for Android core
    // implementation(libs.chrisbanes.haze)        // Blur + haze effects (optional)
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
                from(components["release"])        // Use release build variant
                groupId = "com.zs.compose"         // Maven group ID
                artifactId = "foundation"          // Artifact ID
                version = "3.0.0-dev01"            // Version tag
            }
        }
    }
}