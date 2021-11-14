import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.30"

    `maven-publish`
}

group = "tv.blademaker"
version = "1.2.9"

val jdaVersion = "4.3.0_310"
val coroutinesVersion = "1.5.1"
val logbackVersion = "1.2.5"
val sentryVersion = "5.4.0"

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib", "1.5.30"))
    implementation(kotlin("reflect", "1.5.30"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")

    implementation("org.reflections:reflections:0.9.12")

    api("net.dv8tion:JDA:$jdaVersion") { exclude(module = "opus-java") }
    api("ch.qos.logback:logback-classic:$logbackVersion")
    api("io.sentry:sentry:$sentryVersion")
}

tasks {
    withType<KotlinCompile> {
        this.kotlinOptions.jvmTarget = "11"
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Blad3Mak3r/Slash")

            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }

    publications {
        register<MavenPublication>("grp") {
            artifactId = "slash"
            from(components["java"])
        }
    }
}