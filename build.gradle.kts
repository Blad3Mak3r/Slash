import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.0"

    `maven-publish`
    signing
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
            name = "MavenCentral"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")

            credentials {
                username = System.getenv("CENTRAL_USER")
                password = System.getenv("CENTRAL_PASS")
            }
        }
    }

    publications {
        create<MavenPublication>("MavenCentral") {
            artifactId = "slash"
            groupId = project.group as String
            version = project.version as String
            from(components["java"])

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

signing {
    sign(publishing.publications["MavenCentral"])
}

/*val canSign = getProjectProperty("signing.keyId") != null
if (canSign) {
    signing {
        sign(publishing.publications.getByName("Release"))
    }
}*/