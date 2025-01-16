/*
 * Copyright 2018 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {

    id("com.android.library")
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-android")
    id("kotlin-parcelize")
    id("kotlin-kapt")
    id("maven-publish")
    id("org.jetbrains.dokka")
}

android {
    namespace = "org.readium.r2.shared"
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
                artifactId = "readium-shared"

            }
        }
    }
}


dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.browser)
    implementation(libs.timber)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jsoup)

    // Tests
    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.ext.junit)
    androidTestImplementation(libs.androidx.expresso.core)

    testImplementation(libs.assertj)
    testImplementation(libs.kotlin.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)

    androidTestImplementation(libs.kotlin.junit)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
}
