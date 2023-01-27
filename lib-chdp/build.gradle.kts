@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.com.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
}

android {
    compileSdk = 33
    namespace = "com.healthmetrix.chdp"

    defaultConfig {
        minSdk = 23
        targetSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        debug {
            matchingFallbacks.add("release")
        }

        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.activity)
    implementation(libs.androidx.browser)
    implementation(libs.threetenabp)
    implementation(libs.result)
    implementation(libs.result.coroutines)

    // d4l
    // sdk-android exposes util and core, but I want to wrap the client
    implementation(libs.d4l.sdk.android) {
        exclude(group = "care.data4life.hc-sdk-kmp", module = "securestore-jvm")
        exclude(group = "care.data4life.hc-sdk-kmp", module = "crypto-jvm")
        exclude(group = "care.data4life.hc-sdk-kmp", module = "auth-jvm")
        exclude(group = "care.data4life.hc-util-sdk-kmp", module = "util-jvm")
        exclude(group = "org.threeten", module = "threetenbp")
        exclude(group = "com.google.crypto.tink")
    }

    runtimeOnly(libs.google.crypto.tink)

    api(libs.d4l.sdk.core) {
        exclude(module = "securestore-jvm")
        exclude(module = "crypto-jvm")
        exclude(module = "auth-jvm")
        exclude(module = "util-jvm")
        exclude(module = "error-jvm")
        exclude(group = "care.data4life.hc-util-sdk-kmp", module = "util")
        exclude(group = "org.threeten", module = "threetenbp")
    }
    api(libs.d4l.error)
    api(libs.d4l.util.android)
    api(libs.d4l.fhir)
}
