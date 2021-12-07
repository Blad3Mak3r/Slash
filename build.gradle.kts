import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.0"

    `maven-publish`
}

group = "tv.blademaker"
version = "0.4.1"

val jdaVersion = "4.4.0_350"
val coroutinesVersion = "1.5.2"
val slf4jApi = "1.7.32"
val prometheusVersion = "0.12.0"
val reflectionsVersion = "0.10.2"
val sentryVersion = "5.4.2"

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib", "1.6.0"))
    implementation(kotlin("reflect", "1.6.0"))

    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")

    api("org.reflections:reflections:$reflectionsVersion")

    api("net.dv8tion:JDA:$jdaVersion") { exclude(module = "opus-java") }
    api("org.slf4j:slf4j-api:$slf4jApi")
    api("io.sentry:sentry:$sentryVersion")

    api("io.prometheus:simpleclient:$prometheusVersion")

    testImplementation("junit:junit:4.13.2")
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