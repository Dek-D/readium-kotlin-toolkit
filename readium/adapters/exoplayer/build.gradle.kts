/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

plugins {
    id("readium.library-conventions")
    id("maven-publish")
    kotlin("kapt")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.readium.adapter.exoplayer"
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

dependencies {
    api(project(":readium:adapters:exoplayer:readium-adapter-exoplayer-audio"))
}
