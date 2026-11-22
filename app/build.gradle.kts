plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystoreFile = rootProject.file("apex-route-release")
val hasKeystore = keystoreFile.exists()

android {
    namespace = "com.apexroute.audio"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.apexroute.audio"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    // Rename generated APKs to Apex Route.
    applicationVariants.all {
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "ApexRoute-${versionName}.apk"
        }
    }

    if (hasKeystore) {
        signingConfigs {
            create("release") {
                storeFile = keystoreFile
                storePassword = "apexroute2026"
                keyAlias = "apexroute"
                keyPassword = "apexroute2026"
            }
        }
        buildTypes {
            release {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
