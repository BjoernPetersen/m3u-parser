@file:Suppress("UnstableApiUsage")

import com.diffplug.spotless.LineEnding
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.diffplug.spotless") version Plugin.SPOTLESS
    id("io.gitlab.arturbosch.detekt") version Plugin.DETEKT
    id("com.github.ben-manes.versions") version Plugin.VERSIONS
    jacoco
    idea

    kotlin("jvm") version Plugin.KOTLIN
    `java-library`

    id("org.jetbrains.dokka") version Plugin.DOKKA
    signing
    `maven-publish`
}

group = "com.github.bjoernpetersen"
version = "1.2.0"

repositories {
    jcenter()
}

idea {
    module {
        isDownloadJavadoc = true
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }

    withSourcesJar()
}

spotless {
    kotlin {
        ktlint()
        lineEndings = LineEnding.UNIX
        endWithNewline()
    }
    kotlinGradle {
        ktlint()
        lineEndings = LineEnding.UNIX
        endWithNewline()
    }
    format("markdown") {
        target("**/*.md")
        lineEndings = LineEnding.UNIX
        endWithNewline()
    }
}

detekt {
    toolVersion = Plugin.DETEKT
    config = files("$rootDir/buildConfig/detekt.yml")
    buildUponDefaultConfig = true
}

jacoco {
    toolVersion = Plugin.JACOCO
}

tasks {
    create("javadocJar", Jar::class) {
        dependsOn("dokkaJavadoc")
        archiveClassifier.set("javadoc")
        from("$buildDir/dokka/javadoc")
    }

    "processResources"(ProcessResources::class) {
        filesMatching("**/version.properties") {
            filter {
                it.replace("%APP_VERSION%", version.toString())
            }
        }
    }

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }

    jacocoTestReport {
        reports {
            html.isEnabled = true
            xml.isEnabled = true
            csv.isEnabled = false
        }
    }

    check {
        finalizedBy("jacocoTestReport")
    }

    withType<Jar> {
        from(project.projectDir) {
            include("LICENSE")
        }
    }

    dependencyUpdates {
        rejectVersionIf {
            val version = candidate.version
            isUnstable(version, currentVersion) || isWrongPlatform(version, currentVersion)
        }
    }
}

dependencies {
    api(group = "org.slf4j", name = "slf4j-api", version = Lib.SLF4J)
    implementation(
        group = "io.github.microutils",
        name = "kotlin-logging",
        version = Lib.KOTLIN_LOGGING
    )

    testImplementation(
        group = "org.junit.jupiter",
        name = "junit-jupiter-api",
        version = Lib.JUNIT
    )
    testImplementation(group = "org.assertj", name = "assertj-core", version = Lib.ASSERT_J)
    testImplementation(
        group = "nl.jqno.equalsverifier",
        name = "equalsverifier",
        version = Lib.EQUALSVERIFIER
    )

    testRuntimeOnly(
        group = "org.junit.jupiter",
        name = "junit-jupiter-engine",
        version = Lib.JUNIT
    )
    testRuntimeOnly(
        group = "org.slf4j",
        name = "slf4j-simple",
        version = Lib.SLF4J
    )
}

publishing {
    publications {
        create("Maven", MavenPublication::class) {
            from(components["java"])
            artifact(tasks.getByName("javadocJar"))

            pom {
                name.set("m3u-parser")
                description.set("Library to parse .m3u playlist files.")
                url.set("https://github.com/BjoernPetersen/m3u-parser")

                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/BjoernPetersen/m3u-parser.git")
                    developerConnection.set("scm:git:git@github.com:BjoernPetersen/m3u-parser.git")
                    url.set("https://github.com/BjoernPetersen/m3u-parser")
                }

                developers {
                    developer {
                        id.set("BjoernPetersen")
                        name.set("Björn Petersen")
                        email.set("pheasn@gmail.com")
                        url.set("https://github.com/BjoernPetersen")
                    }
                }
            }
        }
        repositories {
            maven {
                val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
                val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots"
                url = uri(
                    if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl
                    else releasesRepoUrl
                )
                credentials {
                    username = project.properties["ossrh.username"]?.toString()
                    password = project.properties["ossrh.password"]?.toString()
                }
            }
        }
    }
}

signing {
    sign(publishing.publications.getByName("Maven"))
}
