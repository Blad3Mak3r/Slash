import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
}

dependencies {
    implementation(project(":slash-core"))
    ksp(project(":slash-ksp-processor"))

    compileOnly(libs.jda) { exclude(module = "opus-java") }
    compileOnly(libs.coroutines.core)

    testImplementation(libs.jda) { exclude(module = "opus-java") }
    testImplementation(libs.coroutines.core)
    testImplementation(kotlin("test"))
}

tasks {
    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
}

java {
    targetCompatibility = JavaVersion.VERSION_11
    sourceCompatibility = JavaVersion.VERSION_11
}
