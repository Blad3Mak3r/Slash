package io.github.blad3mak3r.slash.gradle

import io.github.blad3mak3r.slash.dsl.CommandRegistry
import io.github.blad3mak3r.slash.gradle.tasks.GenerateSlashRegistryTask
import io.github.blad3mak3r.slash.gradle.tasks.ProcessSlashDefsTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

class SlashPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.apply("org.jetbrains.kotlin.jvm")

        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        val mainSourceSet = sourceSets.getByName("main")
        val kotlinExt = project.extensions.getByType(KotlinJvmProjectExtension::class.java)

        // ── Generated source directories ──────────────────────────────────────
        val generatedHandlersDir =
            project.layout.buildDirectory.dir("generated/slash/handlers")
        val generatedRegistryDir =
            project.layout.buildDirectory.dir("generated/slash/registry")

        // ── Locate slash-dsl JAR/classes from plugin's own classloader ─────────
        // Auto-injected so user projects don't need to declare slash-dsl explicitly.
        val slashDslEntry = findClasspathEntry(CommandRegistry::class.java)

        // ── slashDefs source set: src/slash/kotlin/ ───────────────────────────
        val slashDefsSourceSet = sourceSets.create("slashDefs") { ss ->
            // slashDefs inherits main's compile classpath so JDA types are available
            ss.compileClasspath += mainSourceSet.compileClasspath
            if (slashDslEntry != null) {
                val slashDslFiles = project.files(slashDslEntry)
                ss.compileClasspath += slashDslFiles
                ss.runtimeClasspath += slashDslFiles
            }
        }
        // Add src/slash/kotlin to the Kotlin source set for slashDefs
        kotlinExt.sourceSets.named("slashDefs").configure { kss ->
            kss.kotlin.srcDir("src/slash/kotlin")
        }

        // ── Add generated handlers to main Kotlin source set ──────────────────
        kotlinExt.sourceSets.named("main").configure { kss ->
            kss.kotlin.srcDir(generatedHandlersDir)
        }

        // ── processSlashDefs task (Pass 1) ────────────────────────────────────
        val processSlashDefsTask = project.tasks.register(
            "processSlashDefs",
            ProcessSlashDefsTask::class.java
        ) { task ->
            task.group = "slash"
            task.description = "Generates Abstract*CommandHandler sources from slash DSL definitions"

            val compileSlashDefsKotlin = project.tasks.named(
                "compileSlashDefsKotlin",
                KotlinCompile::class.java
            )
            task.dependsOn(compileSlashDefsKotlin)
            task.slashDefsClasses.set(
                compileSlashDefsKotlin.flatMap { it.destinationDirectory }
            )
            task.slashDefsClasspath.from(slashDefsSourceSet.runtimeClasspath)
            task.outputDir.set(generatedHandlersDir)
        }

        // compileKotlin must run after processSlashDefs (generated files are in main source set)
        project.tasks.named("compileKotlin").configure { it.dependsOn(processSlashDefsTask) }

        // ── slashRegistry source set: generated registry ──────────────────────
        val slashRegistrySourceSet = sourceSets.create("slashRegistry") { ss ->
            // Registry references main classes + compile classpath
            ss.compileClasspath = mainSourceSet.compileClasspath +
                    project.files(
                        project.tasks.named("compileKotlin", KotlinCompile::class.java)
                            .map { it.destinationDirectory }
                    )
        }
        // Add generated registry dir to the Kotlin source set for slashRegistry
        kotlinExt.sourceSets.named("slashRegistry").configure { kss ->
            kss.kotlin.srcDir(generatedRegistryDir)
        }

        // ── generateSlashRegistry task (Pass 2) ───────────────────────────────
        val generateSlashRegistryTask = project.tasks.register(
            "generateSlashRegistry",
            GenerateSlashRegistryTask::class.java
        ) { task ->
            task.group = "slash"
            task.description = "Generates SlashCommandRegistry from compiled handler implementations"

            val compileKotlin = project.tasks.named("compileKotlin", KotlinCompile::class.java)
            task.dependsOn(compileKotlin)
            task.mainClassesDir.set(compileKotlin.flatMap { it.destinationDirectory })
            task.compileClasspath.from(mainSourceSet.compileClasspath)
            task.outputDir.set(generatedRegistryDir)
        }

        // compileSlashRegistryKotlin depends on generateSlashRegistry
        project.tasks.named("compileSlashRegistryKotlin").configure { task ->
            task.dependsOn(generateSlashRegistryTask)
        }

        // ── Wire slashRegistry output into the JAR ────────────────────────────
        project.tasks.named("jar", Jar::class.java).configure { jar ->
            jar.dependsOn(project.tasks.named("compileSlashRegistryKotlin"))
            jar.from(slashRegistrySourceSet.output)
        }
    }

    /**
     * Finds the JAR file or classes directory that contains [clazz] from the plugin's own
     * classloader. Used to auto-inject slash-dsl into the slashDefs compile/runtime classpath
     * so consumer projects don't need an explicit `implementation(slash-dsl)` declaration.
     */
    private fun findClasspathEntry(clazz: Class<*>): File? {
        val resourceName = clazz.name.replace('.', '/') + ".class"
        val url = clazz.classLoader?.getResource(resourceName) ?: return null
        return try {
            when (url.protocol) {
                "jar" -> {
                    // jar:file:/path/to/slash-dsl.jar!/io/github/...class
                    val jarPath = url.path.substringBefore("!").removePrefix("file:")
                    File(jarPath).takeIf { it.exists() }
                }
                "file" -> {
                    // file:/path/to/build/classes/kotlin/main/io/github/...class
                    // Walk up `depth` directories to reach the classes root
                    val depth = resourceName.split("/").size
                    var f = File(url.toURI())
                    repeat(depth) { f = f.parentFile }
                    f.takeIf { it.isDirectory }
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}
