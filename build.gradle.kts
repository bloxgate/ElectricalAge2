import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    java
    kotlin("multiplatform") version "1.5.10" apply false
    jacoco
    id("com.github.johnrengelman.shadow") version "7.0.0"
    idea
    id("org.jetbrains.dokka") version "1.4.30"
}

allprojects {
    version = "0.1.0"
    group = "org.eln2"

    buildscript {
        repositories {
            mavenCentral()
        }
    }

    repositories {
        mavenCentral()
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
        maven(url = "https://kotlin.bintray.com/kotlinx")
    }
}

subprojects {
    apply {
        plugin("java")
        plugin("kotlin")
        plugin("jacoco")
        plugin("com.github.johnrengelman.shadow")
        plugin("idea")
        plugin("org.jetbrains.dokka")
    }

    dependencies {
        implementation("org.apache.commons", "commons-math3", "3.6.1")
        implementation("com.google.protobuf", "protobuf-java", "3.17.3")

        testImplementation("org.assertj", "assertj-core", "3.19.0")
        testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.7.2")
        testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "5.7.2")
    }

    tasks {
        named<Test>("test") {
            useJUnitPlatform()

            // *Always* run tests.
            // Ideally we'd cache the test output and print that instead, but this will do for now.
            outputs.upToDateWhen { false }

            // Print pass/fail for all tests to console, and exceptions if there are any.
            testLogging {
                events =
                    setOf(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.STANDARD_ERROR)
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                showExceptions = true
                showCauses = true
                showStackTraces = true
                // TODO: add the other envvars that can trigger debug output
                showStandardStreams = System.getenv("MODS_ELN_DEBUG") != ""

                // At log-level INFO or DEBUG, print everything.
                debug {
                    events = TestLogEvent.values().toSet()
                }
                info {
                    events = debug.events
                }
            }

            // Print a nice summary afterwards.
            afterSuite(KotlinClosure2<TestDescriptor, TestResult, Unit>({ desc, result ->
                if (desc.parent == null) { // will match the outermost suite
                    val output =
                        "Results: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} passed, ${result.failedTestCount} failed, ${result.skippedTestCount} skipped)"
                    val startItem = "|  "
                    val endItem = "  |"
                    val repeatLength = startItem.length + output.length + endItem.length
                    println(
                        '\n' + "- ".repeat(repeatLength) + '\n' + startItem + output + endItem + '\n' + "-".repeat(
                            repeatLength
                        )
                    )
                }
            }))
        }

        jacocoTestReport {
            reports {
                xml.isEnabled = true
                csv.isEnabled = false
            }
        }

        dokkaHtml {
            dokkaSourceSets.configureEach {
                includeNonPublic.set(true)
            }
        }
    }
}

// By default build everything, put it somewhere convenient, and run the tests.
defaultTasks = mutableListOf("bundle", "test")

tasks {
    create<Copy>("bundle") {
        description = "Copies artifacts to the dist directory"
        group = "Build"

        evaluationDependsOnChildren()

        getTasksByName("shadowJar", true)
            .plus(getTasksByName("jar", true))
            .forEach {
                // Ignore the root jar tasks, they're useless and conflicting.
                if (it.project.rootProject != it.project) {
                    from(it)
                }
            }

        into("dist")
    }

    named<Wrapper>("wrapper") {
        gradleVersion = "7.0.2"
        distributionType = Wrapper.DistributionType.ALL
    }
}
