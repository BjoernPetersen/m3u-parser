import com.diffplug.spotless.LineEnding
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.spotless)
    alias(libs.plugins.versions)
    jacoco
    idea

    kotlin("jvm") version "2.4.10"

    `java-library`

    alias(libs.plugins.dokka.core)
    alias(libs.plugins.dokka.javadoc)
    signing
    `maven-publish`
}

group = "com.github.bjoernpetersen"
version = "1.5.0-SNAPSHOT"

repositories {
    mavenCentral()
}

idea {
    module {
        isDownloadJavadoc = true
    }
}

kotlin {
    jvmToolchain(26)

    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

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

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks {
    register("javadocJar", Jar::class) {
        dependsOn("dokkaGenerateJavadoc")

        archiveClassifier.set("javadoc")
        description = "Build JAR file with Javadoc"
        from(dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
    }

    "processResources"(ProcessResources::class) {
        filesMatching("**/version.properties") {
            filter {
                it.replace("%APP_VERSION%", version.toString())
            }
        }
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

    testRuntimeOnly(libs.junit.engine)
    testRuntimeOnly(libs.junit.launcher)
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
                    if (version.toString().endsWith("SNAPSHOT")) {
                        snapshotsRepoUrl
                    } else {
                        releasesRepoUrl
                    },
                )
                credentials {
                    username = providers.gradleProperty("ossrh.username").orNull
                    password = providers.gradleProperty("ossrh.password").orNull
                }
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications.getByName("Maven"))
}
