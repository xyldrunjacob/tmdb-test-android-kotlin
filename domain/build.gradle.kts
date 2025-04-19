plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("jacoco")
}

android {
    namespace = "com.xyldrun.domain"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildToolsVersion = "34.0.0"
    ndkVersion = "28.0.13004108"
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(project(":core"))
    
    implementation(libs.koin.android)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    
    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext)
}

tasks.register<JacocoReport>("moduleTestReport") {
    dependsOn("testDebugUnitTest", "testReleaseUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    val debugTree = fileTree("$buildDir/tmp/kotlin-classes/debug") {
        exclude(
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "**/android/**/*.*",
            "**/*$[0-9].*"
        )
    }

    classDirectories.setFrom(debugTree)
    sourceDirectories.setFrom("src/main/java", "src/main/kotlin")
    executionData.setFrom(
        fileTree(buildDir) {
            include("jacoco/testDebugUnitTest.exec")
            include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        }
    )
} 