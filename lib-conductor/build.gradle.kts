@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.com.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.org.jetbrains.kotlin.kapt)
}

android {
    compileSdk = 33
    namespace = "com.healthmetrix.myscience.conductor"

    defaultConfig {
        minSdk = 23
        targetSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
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

    implementation(libs.androidx.core)
    implementation(libs.material)
    implementation(projects.commons)

    implementation(libs.androidx.appcompat)

    // little bug with the feature preview I guess
    api(libs.conductor.asProvider()) {
        exclude(group = "org.jetbrains.kotlin")
    }

    api(libs.conductor)
    api(libs.conductor.archlifecycle)

    implementation(libs.androidx.interpolator)
}
