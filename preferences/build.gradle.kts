// Import Kotlin JVM target configuration APIs
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// -----------------------------------------------------------------------------
// 🔌 Plugins
// -----------------------------------------------------------------------------
// Core plugins for Android library development, Compose support,
// and Maven artifact publishing.
plugins {
    alias(libs.plugins.android.library)        // Android Library
    id(libs.plugins.maven.publish.get().pluginId) // Maven Publishing
}

// -----------------------------------------------------------------------------
// 🧠 Kotlin Compiler Configuration
// -----------------------------------------------------------------------------
// Configure JVM target and enable project-wide Kotlin language features.
kotlin {
    compilerOptions {
        // Generate Java 17 compatible bytecode.
        jvmTarget = JvmTarget.JVM_17
        // Enable experimental and advanced Kotlin features used by the project.
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",         // Opt-in APIs
            "-XXLanguage:+NestedTypeAliases",       // Nested type aliases
            "-Xwhen-guards",                        // When guards
            "-Xcompanion-blocks-and-extensions"     // Companion blocks & extensions
        )
    }
}

// -----------------------------------------------------------------------------
// 🤖 Android Configuration
// -----------------------------------------------------------------------------
// Android-specific build, compatibility, and publishing settings.
android {
    namespace = "com.zs.datastore.preferences"
    compileSdk { version = release(37) { minorApiLevel = 2 } }  // Target Android SDK used for compilation.
    defaultConfig { minSdk = 24 }   // Minimum Android version supported by the library.

    // -----------------------------
    // Java Compatibility Options
    // -----------------------------
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Publishing configuration for Maven consumers.
    publishing {
        singleVariant("release") {
            withSourcesJar()  // Publish source code alongside the AAR.
            withJavadocJar()  // Publish generated API documentation.
        }
    }
}

// -----------------------------------------------------------------------------
// 📚 Dependencies
// -----------------------------------------------------------------------------
// External libraries and module dependencies used by this library.
dependencies {

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
                groupId = "com.zs.datastore"                 // Maven group ID
                artifactId = "preferences"         // Artifact ID
                version = "4.0.0-dev"            // Version tag
            }
        }
    }
}