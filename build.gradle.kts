import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.language.jvm.tasks.ProcessResources
import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    kotlin("jvm") version "1.6.0"
    id("org.jetbrains.dokka") version "1.6.0"

    `maven-publish`
    `java-library`
    signing
}

group = "tv.blademaker"
version = Version(0, 5, 0).toString()

val jdaVersion = "4.4.0_350"
val coroutinesVersion = "1.5.2"
val slf4jApi = "1.7.32"
val prometheusVersion = "0.12.0"
val reflectionsVersion = "0.10.2"
val sentryVersion = "5.4.3"

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib", "1.6.0"))
    implementation(kotlin("reflect", "1.6.0"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    api("org.reflections:reflections:$reflectionsVersion")

    api("net.dv8tion:JDA:$jdaVersion") { exclude(module = "opus-java") }
    api("org.slf4j:slf4j-api:$slf4jApi")
    api("io.sentry:sentry:$sentryVersion")

    api("io.prometheus:simpleclient:$prometheusVersion")

    testImplementation("junit:junit:4.13.2")
}

val dokkaOutputDir = "$buildDir/dokka"

tasks {
    withType<KotlinCompile> {
        this.kotlinOptions.jvmTarget = "11"
    }

    getByName<org.jetbrains.dokka.gradle.DokkaTask>("dokkaHtml") {
        outputDirectory.set(file(dokkaOutputDir))
    }

    @Suppress("UnstableApiUsage")
    named<ProcessResources>("processResources") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        val tokens = mapOf(
            "project.version"       to project.version
        )

        from("src/main/resources") {
            include("module.properties")
            //expand(tokens)
            filter<ReplaceTokens>("tokens" to tokens)
        }
    }

    named("build") {
        dependsOn(processResources)
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

publishing {
    repositories {
        maven {
            name = "MavenCentral"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")

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
    private val revision: Int,
    private val classifier: String? = null
) {
    override fun toString(): String {
        return "$major.$minor.$revision" + if (classifier != null) "-$classifier" else ""
    }
}

val canSign = System.getenv("SIGNING_KEY_ID") != null
if (canSign) {
    signing {
        sign(publishing.publications["MavenCentral"])
    }
}