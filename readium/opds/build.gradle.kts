/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    id("com.android.library")
    alias(libs.plugins.ksp)
    id("kotlin-android")
    id("kotlin-parcelize")
    id("kotlin-kapt")
    id("maven-publish")
    id("org.jetbrains.dokka")
}

android {
    namespace = "org.readium.r2.opds"

    compileSdk = 34
    defaultConfig {
        minSdk = 21
        targetSdk = 34
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
        allWarningsAsErrors = true
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components.getByName("release"))
                groupId = "com.github.Dek-D"
                artifactId = "readium-opds"

            }
        }
    }
}

dependencies {
    api(project(":readium:readium-shared"))

    implementation(libs.androidx.appcompat)
    implementation(libs.timber)
    implementation(libs.joda.time)
    implementation(libs.kotlinx.coroutines.android)

    // Tests
    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.ext.junit)
    androidTestImplementation(libs.androidx.expresso.core)

    testImplementation(libs.robolectric)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
}
