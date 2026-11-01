import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// -----------------------------------------------------------------------------
// PLUGINS
// -----------------------------------------------------------------------------
// 📦 Core plugins required for Android Library + Maven publishing.
plugins {
    alias(libs.plugins.android.library)       // Android library plugin
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
    namespace = "com.zs.preferences"
    compileSdk { version = release(37) { minorApiLevel = 2 } }

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
// 📚 Core library: AndroidX DataStore Preferences.
dependencies {
    implementation(libs.androidx.preferences) // DataStore Preferences API
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
                groupId = "com.zs"                 // Maven group ID
                artifactId = "preferences"         // Artifact ID
                version = "3.0.0-dev01"            // Version tag
            }
        }
    }
}