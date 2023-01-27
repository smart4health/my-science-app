import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.org.jetbrains.kotlin.jvm)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.serialization)
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
}

tasks.withType<JavaCompile> {
    targetCompatibility = JavaVersion.VERSION_1_8.toString()
}

dependencies {
    implementation(libs.kotlinx.serialization.json)

    api(libs.retrofit2)
    api(libs.okhttp)

    implementation(libs.retrofit2.converter.kotlinx)
}
