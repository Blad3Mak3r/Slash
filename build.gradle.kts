import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.0"
    id("org.jetbrains.dokka") version "1.6.0"

    `maven-publish`
    `java-library`
    signing
}

group = "tv.blademaker"
val versionObj = Version(0, 8, 0)
version = versionObj.toString()

val jdaVersion = "5.0.0-alpha.12"
val coroutinesVersion = "1.6.0"
val slf4jApi = "1.7.36"
val prometheusVersion = "0.15.0"
val reflectionsVersion = "0.10.2"
val sentryVersion = "5.6.2"

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib", "1.6.0"))
    implementation(kotlin("reflect", "1.6.0"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    implementation("org.reflections:reflections:$reflectionsVersion")

    implementation("net.dv8tion:JDA:$jdaVersion") { exclude(module = "opus-java") }
    api("org.slf4j:slf4j-api:$slf4jApi")
    api("io.sentry:sentry:$sentryVersion")

    api("io.prometheus:simpleclient:$prometheusVersion")

    testImplementation("junit:junit:4.13.2")
    testImplementation("ch.qos.logback:logback-classic:1.2.11")
}

val dokkaOutputDir = "$buildDir/dokka"

tasks {
    withType<KotlinCompile> {
        this.kotlinOptions.jvmTarget = "11"
    }

    getByName<org.jetbrains.dokka.gradle.DokkaTask>("dokkaHtml") {
        outputDirectory.set(file(dokkaOutputDir))
    }
}

val deleteDokkaOutputDir by tasks.register<Delete>("deleteDokkaOutputDirectory") {
    delete(dokkaOutputDir)
}

val javadocJar = tasks.register<Jar>("javadocJar") {
    dependsOn(deleteDokkaOutputDir, tasks.dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaOutputDir)
}

java {
    withSourcesJar()
}

val mavenCentralRepository = if (versionObj.isSnapshot)
    "https://s01.oss.sonatype.org/content/repositories/snapshots/"
else
    "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"

publishing {
    repositories {
        maven {
            name = "MavenCentral"
            url = uri(mavenCentralRepository)

            credentials {
                username = System.getenv("OSSRH_USERNAME")
                password = System.getenv("OSSRH_PASSWORD")
            }
        }
    }

    publications {
        create<MavenPublication>("MavenCentral") {
            artifactId = "slash"
            groupId = project.group as String
            version = project.version as String
            from(components["java"])
            artifact(javadocJar)

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

class Version(
    private val major: Int,
    private val minor: Int,
    private val revision: Int
) {
    val isSnapshot = System.getenv("OSSRH_SNAPSHOT") != null

    override fun toString(): String {
        return "$major.$minor.$revision" + if (isSnapshot) "-SNAPSHOT" else ""
    }
}

val canSign = System.getenv("SIGNING_KEY_ID") != null
if (canSign) {
    signing {
        sign(publishing.publications["MavenCentral"])
    }
}
