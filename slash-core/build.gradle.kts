import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.publish)
    alias(libs.plugins.versions)

    `java-library`
    signing
    java
}

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib", "2.3.0"))
    implementation(kotlin("reflect", "2.3.0"))

    compileOnly(libs.coroutines.core)
    api(libs.reflections)
    compileOnly(libs.jda) { exclude(module = "opus-java") }
    implementation(libs.slf4j)
    implementation(libs.sentry)
    compileOnly(libs.prometheus)

    testImplementation("junit:junit:4.13.2")
    testImplementation("ch.qos.logback:logback-classic:1.5.22")
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
    coordinates("io.github.blad3mak3r.slash", "slash-core", "$version")

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
