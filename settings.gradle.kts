rootProject.name = "KBlobs"

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
    }
}

include(":kblobs")

include(":samples:shared")
include(":samples:androidApp")
