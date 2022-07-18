@file:Suppress("UnstableApiUsage")

import com.diffplug.spotless.LineEnding
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
    jacoco
    idea

    kotlin("jvm") version "1.7.10"
    `java-library`

    alias(libs.plugins.dokka)
    signing
    `maven-publish`
}

group = "com.github.bjoernpetersen"
version = "1.3.0"

repositories {
    mavenCentral()
}

idea {
    module {
        isDownloadJavadoc = true
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }

    withSourcesJar()
}

spotless {
    kotlin {
        ktlint()
            .editorConfigOverride(
                mapOf(
                    "ij_kotlin_allow_trailing_comma" to "true",
                    "ij_kotlin_allow_trailing_comma_on_call_site" to "true",
                ),
            )
        lineEndings = LineEnding.UNIX
        endWithNewline()
    }
    kotlinGradle {
        ktlint()
            .editorConfigOverride(
                mapOf(
                    "ij_kotlin_allow_trailing_comma" to "true",
                    "ij_kotlin_allow_trailing_comma_on_call_site" to "true",
                ),
            )
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
    toolVersion = libs.versions.detekt.get()
    config = files("$rootDir/buildConfig/detekt.yml")
    buildUponDefaultConfig = true
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
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

    withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
        options.release.set(8)
    }

    withType<Test> {
        useJUnitPlatform()
    }

    jacocoTestReport {
        reports {
            html.required.set(true)
            xml.required.set(true)
            csv.required.set(false)
        }
    }

    check {
        finalizedBy("jacocoTestReport")
    }

    withType<Jar> {
        from(project.projectDir) {
            include("LICENSE")
        }

        manifest {
            attributes("Automatic-Module-Name" to "net.bjoernpetersen.m3u")
        }
    }
}

dependencies {
    api(libs.slf4j.api)
    implementation(libs.kotlin.logging)

    testImplementation(libs.junit.api)
    testImplementation(libs.assertj.core)
    testImplementation(libs.equalsverifier)

    testRuntimeOnly(libs.junit.engine)
    testRuntimeOnly(libs.slf4j.simple)
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
                        email.set("git@bjoernpetersen.net")
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
                    else releasesRepoUrl,
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
