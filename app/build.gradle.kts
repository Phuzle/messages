plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics.plugin)
}

// CI (see .github/workflows/release.yml) overrides these via -PreleaseVersionCode/-PreleaseVersionName
// when building from a published release tag; local/default builds keep the checked-in values.
val releaseVersionCode = (project.findProperty("releaseVersionCode") as String?)?.toIntOrNull() ?: 1
val releaseVersionName = project.findProperty("releaseVersionName") as String? ?: "0.0.1"

// Release signing is CI-only, sourced from GitHub Actions secrets (see .github/workflows/release.yml)
// and never committed — local `assembleRelease` builds stay unsigned when these aren't set, same as
// before this existed, so no developer setup is required for anything except an actual GitHub release.
val releaseKeystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
val hasReleaseSigning = !releaseKeystorePath.isNullOrBlank()

android {
    namespace = "com.phuzle.labs.messages"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.phuzle.labs.messages"
        minSdk = 26
        targetSdk = 36
        versionCode = releaseVersionCode
        versionName = releaseVersionName

        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // R8 shrinks and obfuscates; the mapping file is auto-uploaded to Firebase Crashlytics
            // by the Crashlytics Gradle plugin and to Play Console via the CI workflow.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
            // Embed native debug symbols from dependency .so files (e.g. OkHttp, Firebase) so
            // that Play Console can symbolicate native crashes and ANRs.
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Per-ABI release APKs for direct-download (GitHub Release) distribution, alongside a
    // universal one — this app has no native/NDK code, so these are all functionally identical
    // and nearly the same size as each other; the split exists for the release naming convention
    // release.yml expects, not because it shrinks anything. Play Store gets the AAB (bundleRelease)
    // instead, which handles per-device delivery on its own — this config has no effect on that.
    // Scoped to release builds only (via the invoked task names) — the `splits` block otherwise
    // applies to every build type including debug, which turned local `assembleDebug`/installs
    // into five APKs instead of the single app-debug.apk every install script here expects.
    val isBuildingReleaseApk = gradle.startParameter.taskNames.any { it.contains("assembleRelease", ignoreCase = true) }
    splits {
        abi {
            isEnable = isBuildingReleaseApk
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Kotlin compiler options — replaces the removed kotlinOptions DSL (deprecated in Kotlin 2.1,
// removed in Kotlin 2.3). Setting jvmTarget here applies to all compilations in this module.
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.coil.compose)
    implementation(libs.androidx.biometric)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.browser)
    implementation(libs.play.app.update)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.config)
}
