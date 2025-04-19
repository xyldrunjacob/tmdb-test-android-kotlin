// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jacoco:org.jacoco.core:0.8.11")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.23" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

// Apply common configurations to all projects
allprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
}

// Apply Jacoco plugin to all projects
subprojects {
    afterEvaluate {
        if (plugins.hasPlugin("com.android.library") || plugins.hasPlugin("com.android.application")) {
            apply(plugin = "jacoco")
            
            extensions.configure<JacocoPluginExtension> {
                toolVersion = "0.8.11"
            }
            
            tasks.withType<Test> {
                configure<JacocoTaskExtension> {
                    isIncludeNoLocationClasses = true
                    excludes = listOf("jdk.internal.*")
                }
            }

            // Configure test coverage for each module
            tasks.register<JacocoReport>("${project.name}TestReport") {
                dependsOn("testDebugUnitTest")
                
                reports {
                    xml.required.set(true)
                    html.required.set(true)
                }

                val fileFilter = listOf(
                    "**/R.class",
                    "**/R$*.class",
                    "**/BuildConfig.*",
                    "**/Manifest*.*",
                    "**/*Test*.*",
                    "android/**/*.*"
                )

                val debugTree = fileTree("${project.buildDir}/tmp/kotlin-classes/debug") {
                    exclude(fileFilter)
                }

                val mainSrc = "${project.projectDir}/src/main/java"

                sourceDirectories.setFrom(files(mainSrc))
                classDirectories.setFrom(files(debugTree))
                executionData.setFrom(fileTree(project.buildDir) {
                    include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
                })
            }

            tasks.withType<Test> {
                finalizedBy(tasks.named("${project.name}TestReport"))
            }
        }
    }
}

// Create a task to merge all Jacoco reports
tasks.register<JacocoReport>("jacocoFullReport") {
    dependsOn(":app:testDebugUnitTest", ":data:testDebugUnitTest", ":presentation:testDebugUnitTest", ":core:testDebugUnitTest", ":domain:testDebugUnitTest")
    
    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/*ComposableSingletons*.*",
        "**/*Composable*.*",
        "**/*Preview*.*"
    )

    // Create build directory if it doesn't exist
    doFirst {
        mkdir("${project.buildDir}/reports/jacoco/jacocoFullReport")
    }

    val classFiles = files(
        fileTree("app/build/tmp/kotlin-classes/debug") {
            exclude(fileFilter)
        },
        fileTree("data/build/tmp/kotlin-classes/debug") {
            exclude(fileFilter)
        },
        fileTree("presentation/build/tmp/kotlin-classes/debug") {
            exclude(fileFilter)
        },
        fileTree("core/build/tmp/kotlin-classes/debug") {
            exclude(fileFilter)
        },
        fileTree("domain/build/tmp/kotlin-classes/debug") {
            exclude(fileFilter)
        }
    )

    val sourceFiles = files(
        "app/src/main/kotlin",
        "app/src/main/java",
        "data/src/main/kotlin",
        "data/src/main/java",
        "presentation/src/main/kotlin",
        "presentation/src/main/java",
        "core/src/main/kotlin",
        "core/src/main/java",
        "domain/src/main/kotlin",
        "domain/src/main/java"
    )

    val executionFiles = files(
        fileTree("app/build") {
            include(
                "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
                "jacoco/testDebugUnitTest.exec"
            )
        },
        fileTree("data/build") {
            include(
                "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
                "jacoco/testDebugUnitTest.exec"
            )
        },
        fileTree("presentation/build") {
            include(
                "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
                "jacoco/testDebugUnitTest.exec"
            )
        },
        fileTree("core/build") {
            include(
                "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
                "jacoco/testDebugUnitTest.exec"
            )
        },
        fileTree("domain/build") {
            include(
                "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
                "jacoco/testDebugUnitTest.exec"
            )
        }
    )

    classDirectories.setFrom(classFiles)
    sourceDirectories.setFrom(sourceFiles)
    executionData.setFrom(executionFiles)

    reports {
        xml.required.set(true)
        xml.outputLocation.set(file("${project.buildDir}/reports/jacoco/jacocoFullReport/jacocoFullReport.xml"))
        html.required.set(true)
        html.outputLocation.set(file("${project.buildDir}/reports/jacoco/jacocoFullReport/html"))
        csv.required.set(false)
    }
}

// Configure jacocoAnt for the classpath
configurations {
    create("jacocoAnt")
}

dependencies {
    "jacocoAnt"("org.jacoco:org.jacoco.ant:0.8.11")
}

tasks.withType<JacocoReport> {
    this.jacocoClasspath = configurations["jacocoAnt"]
}

// Optional: Task to generate coverage verification
tasks.register<JacocoCoverageVerification>("verifyCoverage") {
    dependsOn("jacocoFullReport")
    
    violationRules {
        rule {
            limit {
                minimum = "0.70".toBigDecimal()
            }
        }
        
        rule {
            element = "CLASS"
            excludes = listOf(
                "*.BuildConfig.*",
                "*.R.*"
            )
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}