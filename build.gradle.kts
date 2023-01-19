import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream

plugins {
    kotlin("jvm") version "1.8.0"
    id("org.jetbrains.dokka") version "1.7.20"
    id("com.github.ben-manes.versions") version "0.44.0"

    `maven-publish`
    `java-library`
    signing
}

group = "tv.blademaker"

val gitTag: String? by lazy {
    try {
        val stdout = ByteArrayOutputStream()
        rootProject.exec {
            commandLine("git", "describe", "--tags", "--abbrev=0")
            standardOutput = stdout
        }

        stdout.toString().trim()
    } catch(e: Throwable) {
        null
    }
}

val gitHash: String by lazy {
    val stdout = ByteArrayOutputStream()
    rootProject.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        standardOutput = stdout
    }

    stdout.toString().trim()
}

val isSnapshot = System.getenv("OSSRH_SNAPSHOT") != null

version = (gitTag ?: gitHash).plus(if (isSnapshot) "-SNAPSHOT" else "")

val jdaVersion = "5.0.0-beta.3"
val coroutinesVersion = "1.6.4"
val slf4jApi = "2.0.6"
val prometheusVersion = "0.16.0"
val reflectionsVersion = "0.10.2"
val sentryVersion = "6.12.1"

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib", "1.7.21"))
    implementation(kotlin("reflect", "1.7.21"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    implementation("org.reflections:reflections:$reflectionsVersion")

    implementation("net.dv8tion:JDA:$jdaVersion") { exclude(module = "opus-java") }
    api("org.slf4j:slf4j-api:$slf4jApi")
    api("io.sentry:sentry:$sentryVersion")

    api("io.prometheus:simpleclient:$prometheusVersion")

    testImplementation("junit:junit:4.13.2")
    testImplementation("ch.qos.logback:logback-classic:1.4.5")
}

val dokkaOutputDir = "$buildDir/dokka"

tasks {
    withType<KotlinCompile> {
        this.kotlinOptions.jvmTarget = "11"
    }

    getByName<org.jetbrains.dokka.gradle.DokkaTask>("dokkaHtml") {
        outputDirectory.set(file(dokkaOutputDir))
    }
}

val deleteDokkaOutputDir by tasks.register<Delete>("deleteDokkaOutputDirectory") {
    delete(dokkaOutputDir)
}

val javadocJar = tasks.register<Jar>("javadocJar") {
    dependsOn(deleteDokkaOutputDir, tasks.dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaOutputDir)
}

java {
    targetCompatibility = JavaVersion.VERSION_11
    sourceCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
}

val mavenCentralRepository = if (isSnapshot)
    "https://s01.oss.sonatype.org/content/repositories/snapshots/"
else
    "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"

publishing {
    repositories {
        maven {
            name = "MavenCentral"
            url = uri(mavenCentralRepository)

            credentials {
                username = System.getenv("OSSRH_USERNAME")
                password = System.getenv("OSSRH_PASSWORD")
            }
        }
    }

    publications {
        create<MavenPublication>("MavenCentral") {
            artifactId = "slash"
            groupId = project.group as String
            version = project.version as String
            from(components["java"])
            artifact(javadocJar)

            pom {
                name.set(project.name)
                description.set("Advanced Slash Command handler for Discord and JDA")
                url.set("https://github.com/Blad3Mak3r/Slash")
                issueManagement {
                    system.set("GitHub")
                    url.set("https://github.com/Blad3Mak3r/Slash/issues")
                }
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://github.com/Blad3Mak3r/Slash/LICENSE.txt")
                        distribution.set("repo")
                    }
                }
                scm {
                    url.set("https://github.com/Blad3Mak3r/Slash")
                    connection.set("https://github.com/Blad3Mak3r/Slash.git")
                    developerConnection.set("scm:git:ssh://git@github.com:Blad3Mak3r/Slash.git")
                }
                developers {
                    developer {
                        name.set("Juan Luis Caro")
                        url.set("https://github.com/Blad3Mak3r")
                    }
                }
            }
        }
    }
}

val canSign = System.getenv("SIGNING_KEY_ID") != null
if (canSign) {
    signing {
        sign(publishing.publications["MavenCentral"])
    }
}
