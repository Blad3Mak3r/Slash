plugins {
    kotlin("jvm") version "1.5.30"

    `maven-publish`
}

group = "tv.blademaker"
version = "1.0"

val jdaVersion = "4.3.0_310"
val coroutinesVersion = "1.5.1"
val logbackVersion = "1.2.5"
val sentryVersion = "5.1.2"

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
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
