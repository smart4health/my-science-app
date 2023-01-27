import com.android.build.api.dsl.ApplicationBuildType
import com.android.build.api.variant.BuiltArtifactsLoader
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly
import java.util.Properties

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.com.android.application)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.org.jetbrains.kotlin.kapt)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.serialization)
    alias(libs.plugins.com.google.gms.googleservices)
    alias(libs.plugins.com.google.protobuf)
    alias(libs.plugins.com.squareup.sqldelight)
    alias(libs.plugins.com.google.android.gms.osslicensesplugin)
    alias(libs.plugins.com.google.firebase.crashlytics)
    alias(libs.plugins.com.google.dagger.hilt.android)
}

fun Project.loadProperties(path: String): Properties? = try {
    this.file(path)
        .inputStream()
        .use { inputStream ->
            Properties().also { it.load(inputStream) }
        }
} catch (ex: Exception) {
    null
}

fun Project.propertyMap(filename: String): Map<String, String> {
    val props = try {
        File(projectDir, filename)
            .inputStream()
            .use { inputStream -> Properties().also { it.load(inputStream) } }
    } catch (ex: Exception) {
        error("$filename could not be read")
    }

    return props
        .mapKeys { it.key as String }
        .mapValues { it.value as String }
}

val keystore = when (val e = System.getenv("SIGNING_CONFIG")) {
    "release" -> rootProject.loadProperties("keystore.properties")
        ?: error("Failed to load keystore.properties")
    "upload" -> rootProject.loadProperties("upload-keystore.properties")
        ?: error("Failed to load upload-keystore.properties")
    else -> {
        logger.warn("No signing config selected: $e")
        null
    }
}

open class CopyApkTask : DefaultTask() {

    @get:InputFiles
    var dir: Provider<Directory>? = null

    @get:Internal
    var builtArtifactsLoader: BuiltArtifactsLoader? = null

    @get:Internal
    var outputFileName: String? = null

    @TaskAction
    fun rename() {
        val dir = dir!!.get()
        val builtArtifactsLoader = builtArtifactsLoader!!

        val src = builtArtifactsLoader.load(dir)!!.elements.single().outputFile.let(::File)
        val dest = File(dir.asFile, outputFileName!!)

        src.copyTo(dest, overwrite = true)
    }
}

open class CopyAabTask : DefaultTask() {

    @get:InputFile
    var file: Provider<File>? = null

    @get:Internal
    var outputFileName: String? = null

    @TaskAction
    fun rename() {
        val src = file!!.get()
        val dest = File(src.parentFile, outputFileName!!)

        src.copyTo(dest, overwrite = true)
    }
}

fun ApplicationBuildType.configureSecrets(secrets: Map<String, String>) {
    secrets.filter { it.key.startsWith("data-prov.") }
        .mapKeys { entry ->
            entry.key
                .removePrefix("data-prov.")
                .replace(".", "_")
                .toUpperCaseAsciiOnly()
        }
        .forEach { entry ->
            buildConfigField("String", entry.key, "\"${entry.value}\"")
        }

    secrets.filter { it.key.startsWith("chdp.") }
        .mapKeys { it.key.removePrefix("chdp.") }
        .let(this::addManifestPlaceholders)
}

android {
    compileSdk = 33
    namespace = "com.healthmetrix.s4h.myscience"

    defaultConfig {

        minSdk = 23
        targetSdk = 33

        versionCode = 10
        versionName = "1.6.1"
    }

    signingConfigs {
        if (keystore != null) create("release") {
            keyAlias = keystore.getProperty("key_alias")
            keyPassword = keystore.getProperty("key_password")
            storeFile = rootProject.file(keystore.getProperty("keystore"))
            storePassword = keystore.getProperty("keystore_password")
        }
    }

    buildTypes {
        debug {
            rootProject.propertyMap("debug.secrets.properties")
                .let { this.configureSecrets(it) }

            matchingFallbacks.add("release")
        }

        release {
            isMinifyEnabled = false
            signingConfig = if (keystore != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }

            rootProject.propertyMap("release.secrets.properties")
                .let { this.configureSecrets(it) }

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        viewBinding = true
    }

    // https://old.reddit.com/r/androiddev/comments/ompa5v/is_gradle_configuration_avoidance_possible_with/
    androidComponents {
        onVariants { variant ->

            val suffix = if (variant.name == "release") {
                if (keystore != null) "-signed" else "-unsigned"
            } else ""

            project.tasks.register<CopyApkTask>("copy${variant.buildType?.capitalizeAsciiOnly()}Apk") {
                dir = variant.artifacts.get(com.android.build.api.artifact.SingleArtifact.APK)
                builtArtifactsLoader = variant.artifacts.getBuiltArtifactsLoader()
                outputFileName =
                    "${variant.applicationId.get()}-v${variant.outputs.single().versionName.get()}-${variant.name}$suffix.apk"

                dependsOn("assemble${variant.buildType?.capitalizeAsciiOnly()}")
            }

            project.tasks.register<CopyAabTask>("copy${variant.buildType?.capitalizeAsciiOnly()}Bundle") {
                file = variant.artifacts
                    .get(com.android.build.api.artifact.SingleArtifact.BUNDLE)
                    .map(RegularFile::getAsFile)
                outputFileName =
                    "${variant.applicationId.get()}-v${variant.outputs.single().versionName.get()}-${variant.name}$suffix.aab"

                dependsOn("bundle${variant.buildType?.capitalizeAsciiOnly()}")
            }
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kapt {
    correctErrorTypes = true
}

@Suppress("UnstableApiUsage")
fun MinimalExternalModuleDependency.coordinates(): String {
    return "$module:$versionConstraint"
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().coordinates()
    }

    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {

    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    implementation(libs.hilt)
    kapt(libs.hilt.compiler)

    implementation(projects.commons)

    implementation(projects.libConductor)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle)

    implementation(libs.androidx.datastore)
    implementation(libs.protobuf.javalite)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.result)
    implementation(libs.result.coroutines)

    implementation(libs.firebase.auth)
    implementation(libs.kotlinx.coroutines.play)

    implementation(projects.serviceConsent)
    implementation(projects.serviceRecontact)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)

    implementation(projects.libChdp)

    implementation(libs.androidx.activity)
    runtimeOnly(libs.androidx.fragment)

    implementation(libs.androidx.work)

    implementation(libs.sqldelight.android)
    implementation(libs.sqldelight.coroutines)

    implementation(libs.androidx.swiperefresh)

    implementation(libs.threetenabp)

    implementation(libs.hapi.base)
    implementation(libs.hapi.structures.r4)
    runtimeOnly(libs.caffeine)
    runtimeOnly(libs.fhir.ucum) {
        exclude(group = "junit", module = "junit")
    }

    implementation(projects.serviceDeident)
    implementation(projects.serviceQomop)

    implementation(libs.firebase.crashlytics)

    // Testing
    testImplementation(libs.bundles.test.implementation)
    testRuntimeOnly(libs.bundles.test.runtime)
    testImplementation(libs.threetenabp)
    testImplementation(libs.sqldelight.driver)

    debugRuntimeOnly(libs.leakcanary)

    implementation(libs.osslicenses)
}
