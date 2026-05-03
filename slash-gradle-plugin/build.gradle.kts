import org.gradle.plugin.devel.tasks.PluginUnderTestMetadata
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

// Extra classpath injected into TestKit builds so that Gradle can load
// KotlinSourceSet / KotlinJvmProjectExtension when instantiating SlashPlugin.
// Using a separate config keeps these JARs OUT of the published plugin artifact.
val pluginUnderTestExtra: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.kotlinpoet)
    implementation(libs.kotlin.embeddable)
    implementation(project(":slash-dsl"))

    compileOnly(libs.jda) { exclude(module = "opus-java") }
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())
    compileOnly(kotlin("gradle-plugin"))

    // kotlin-gradle-plugin added only to TestKit classpath (not published runtime)
    pluginUnderTestExtra(kotlin("gradle-plugin"))

    testImplementation(gradleTestKit())
    testImplementation("junit:junit:4.13.2")

    "functionalTestImplementation"(gradleTestKit())
    "functionalTestImplementation"("junit:junit:4.13.2")
}

// Inject the Kotlin Gradle Plugin into the metadata that TestKit reads.
tasks.named<PluginUnderTestMetadata>("pluginUnderTestMetadata") {
    pluginClasspath.from(pluginUnderTestExtra)
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
    // Ensure slash-dsl and slash-runtime JARs are built before functional tests run
    dependsOn(":slash-dsl:jar", ":slash-runtime:jar")
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
