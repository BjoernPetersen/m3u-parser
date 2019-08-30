import com.diffplug.spotless.LineEnding
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.diffplug.gradle.spotless") version Plugin.SPOTLESS
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

group = "net.bjoernpetersen"
version = "0.1.0-SNAPSHOT"

repositories {
    jcenter()
}

idea {
    module {
        isDownloadJavadoc = true
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
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
    "dokka"(DokkaTask::class) {
        outputFormat = "html"
        outputDirectory = "$buildDir/kdoc"
    }

    @Suppress("UNUSED_VARIABLE")
    val dokkaJavadoc by creating(DokkaTask::class) {
        outputFormat = "javadoc"
        outputDirectory = "$buildDir/javadoc"
    }

    @Suppress("UNUSED_VARIABLE")
    val javadocJar by creating(Jar::class) {
        dependsOn("dokkaJavadoc")
        archiveClassifier.set("javadoc")
        from("$buildDir/javadoc")
    }

    @Suppress("UNUSED_VARIABLE")
    val sourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
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
            freeCompilerArgs = listOf(
                "-Xuse-experimental=kotlin.Experimental",
                "-Xnew-inference"
            )
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
}

dependencies {
    api(kotlin("stdlib-jdk8"))
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
            artifact(tasks.getByName("sourcesJar"))

            pom {
                name.set("MusicBot")
                description.set("Library to parse .m3u playlist files.")
                // TODO adjust
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
