import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.com.github.benmanes.versions)
    alias(libs.plugins.com.android.application) apply false
    alias(libs.plugins.com.android.library) apply false
    alias(libs.plugins.org.jetbrains.kotlin.android) apply false
    alias(libs.plugins.org.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.org.jetbrains.kotlin.kapt) apply false
    alias(libs.plugins.org.jetbrains.kotlin.plugin.serialization) apply false
    alias(libs.plugins.com.google.firebase.crashlytics) apply false
    alias(libs.plugins.com.google.protobuf) apply false
    alias(libs.plugins.com.google.gms.googleservices) apply false

    // There's something weird going on with the gradle plugins, where not-applying
    // in the root project is required, if the plugin's dependencies are also not-applied
    // here.  so if the AGP is not-applied in root *and* applied in :app, we still need to
    // not-apply oss-licenses-plugin in root.  This doesn't match up with my intuition around
    // dependencies in general which is why it is confusing.  Not exactly sure why sqldelight
    // appears to be an exception here, but it only appears to have a compile time dependency
    // on the android plugin.  It does have an implementation dependency on kotlin though...
    alias(libs.plugins.com.google.android.gms.osslicensesplugin) apply false
    alias(libs.plugins.com.google.dagger.hilt.android) apply false
    alias(libs.plugins.com.squareup.sqldelight) apply false
}
allprojects {
    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
        }
    }
}

task<Delete>("clean") {
    delete(rootProject.buildDir)
}
