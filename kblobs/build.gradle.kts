import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
}

kotlin {
    explicitApi()

    android {
        namespace = "dev.piotrprus.kblobs"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }

        withHostTest {}
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.ui)
            implementation(libs.compose.foundation)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates("io.github.piotrprus", "kblobs", "0.1.0")

    pom {
        name.set("KBlobs")
        description.set("Compose Multiplatform morphing blobs for Android and iOS: layered, wobbling outlines around any shape, with per-layer color, alpha, blend mode, blur, segments, amplitude range, tempo and rotation.")
        inceptionYear.set("2026")
        url.set("https://github.com/PiotrPrus/KBlobs/")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("piotrprus")
                name.set("Piotr Prus")
                url.set("https://github.com/PiotrPrus/")
            }
        }

        scm {
            url.set("https://github.com/PiotrPrus/KBlobs/")
            connection.set("scm:git:git://github.com/PiotrPrus/KBlobs.git")
            developerConnection.set("scm:git:ssh://git@github.com/PiotrPrus/KBlobs.git")
        }
    }
}

dokka {
    moduleName.set("KBlobs")

    dokkaSourceSets.configureEach {
        // Link generated pages back to the source on GitHub.
        sourceLink {
            localDirectory.set(file("src"))
            remoteUrl("https://github.com/PiotrPrus/KBlobs/tree/main/kblobs/src")
            remoteLineSuffix.set("#L")
        }
        // Surface the README on the module's landing page.
        includes.from("Module.md")
    }

    pluginsConfiguration.html {
        footerMessage.set("© 2026 Piotr Prus · KBlobs — Apache 2.0")
    }
}
