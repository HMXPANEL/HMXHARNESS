pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") { content {
            includeGroup("com.github.topjohnwu.libsu")
            includeGroup("com.github.styropyr0") // PrismalAGSL liquid glass
        } }
    }
}
rootProject.name = "HMXHARNESS"
include(":app")
if (providers.gradleProperty("distribution").orNull == "play") include(":local_gpu")
