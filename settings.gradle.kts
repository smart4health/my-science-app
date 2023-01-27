@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

    resolutionStrategy {
        eachPlugin {
            // not merged https://github.com/google/play-services-plugins/pull/222
            if (requested.id.id == "com.google.android.gms.oss-licenses-plugin") {
                useModule("com.google.android.gms:oss-licenses-plugin:${requested.version}")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        maven { url = uri("https://jitpack.io") }

        val secrets = File(rootProject.projectDir, "secrets.properties")
            .inputStream()
            .use { inputStream -> java.util.Properties().also { it.load(inputStream) } }

        listOf(
            "https://maven.pkg.github.com/d4l-data4life/hc-util-sdk-kmp",
            "https://maven.pkg.github.com/d4l-data4life/hc-fhir-sdk-java",
            "https://maven.pkg.github.com/d4l-data4life/hc-fhir-helper-sdk-kmp",
        ).forEach { path ->
            maven {
                url = uri(path)
                credentials {
                    username = secrets.getProperty("gpr.user")
                        ?: System.getenv("PACKAGE_REGISTRY_USERNAME")

                    password = secrets.getProperty("gpr.token")
                        ?: System.getenv("PACKAGE_REGISTRY_TOKEN")
                }
            }
        }
    }

    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}

rootProject.name = "my-science"
include(":app")
include(":commons")
include(":lib-conductor")
include(":lib-chdp")
include(":service-consent")
include(":service-recontact")
include(":service-deident")
include(":service-qomop")
