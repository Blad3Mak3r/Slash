import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.kotlin.jvm)
    id("com.github.ben-manes.versions") version "0.51.0"

    id("com.vanniktech.maven.publish") version "0.34.0"

    `java-library`
    signing
    java
}

group = "io.github.blad3mak3r"

val gitTag: String? by lazy {
    try {
        val stdout = ByteArrayOutputStream()
        rootProject.exec {
            commandLine("git", "describe", "--tags", "--abbrev=0")
            standardOutput = stdout
        }

        stdout.toString().trim()
    } catch(_: Throwable) {
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

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib", "2.0.20"))
    implementation(kotlin("reflect", "2.0.20"))

    compileOnly(libs.coroutines.core)
    api(libs.reflections)
    compileOnly(libs.jda) { exclude(module = "opus-java") }
    implementation(libs.slf4j)
    implementation(libs.sentry)
    compileOnly(libs.prometheus)

    testImplementation("junit:junit:4.13.2")
    testImplementation("ch.qos.logback:logback-classic:1.5.13")
}

tasks {
    withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.add("-Xjsr305=strict")
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
}

java {
    targetCompatibility = JavaVersion.VERSION_11
    sourceCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
}


mavenPublishing {
    coordinates("io.github.blad3mak3r", "slash", "$version")

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

    publishToMavenCentral(automaticRelease = true)

    signAllPublications()
}
