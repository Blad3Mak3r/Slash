import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.publish)

    `java-library`
    signing
    java
}

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
}

dependencies {
    compileOnly(libs.jda) { exclude(module = "opus-java") }
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
    withSourcesJar()
}

mavenPublishing {
    coordinates("io.github.blad3mak3r.slash", "slash-annotations", "$version")

    pom {
        name.set(project.name)
        description.set("Compile-time annotations for the Slash Discord command library")
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
