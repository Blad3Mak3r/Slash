import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.versions)
    `java-gradle-plugin`
    `maven-publish`
}

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
}

val functionalTest: SourceSet = sourceSets.create("functionalTest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

configurations["functionalTestImplementation"]
    .extendsFrom(configurations["testImplementation"])

dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.kotlinpoet)
    implementation(libs.kotlin.embeddable)
    implementation(project(":slash-dsl"))

    compileOnly(libs.jda) { exclude(module = "opus-java") }
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())
    compileOnly(kotlin("gradle-plugin"))

    testImplementation(gradleTestKit())
    testImplementation("junit:junit:4.13.2")

    "functionalTestImplementation"(gradleTestKit())
    "functionalTestImplementation"("junit:junit:4.13.2")
}

gradlePlugin {
    plugins {
        create("slashPlugin") {
            id = "io.github.blad3mak3r.slash"
            implementationClass = "io.github.blad3mak3r.slash.gradle.SlashPlugin"
            displayName = "Slash Command Plugin"
            description = "Compile-time Slash Command code generation for JDA"
        }
    }

    testSourceSets(functionalTest)
}

val functionalTestTask = tasks.register<Test>("functionalTest") {
    group = "verification"
    testClassesDirs = functionalTest.output.classesDirs
    classpath = functionalTest.runtimeClasspath
    mustRunAfter(tasks.test)
}

tasks.check { dependsOn(functionalTestTask) }

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
