package io.github.blad3mak3r.slash.gradle.tasks

import io.github.blad3mak3r.slash.gradle.codegen.AbstractHandlerGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*
import java.net.URLClassLoader

/**
 * **Pass 1** — Compile-time code generation.
 *
 * Reads compiled classes from `src/slash/kotlin/` (the `slashDefs` source set), loads them via
 * [URLClassLoader] to populate `CommandRegistry`, then generates one `Abstract*CommandHandler.kt`
 * per command definition using KotlinPoet.
 *
 * The generated files are written to [outputDir] which is added to the `main` source set so
 * `compileKotlin` picks them up automatically.
 */
@CacheableTask
abstract class ProcessSlashDefsTask : DefaultTask() {

    /** Compiled output of the `slashDefs` source set (contains user-defined CommandDef instances). */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val slashDefsClasses: DirectoryProperty

    /**
     * Full runtime classpath for the slashDefs source set (slash-dsl jar + compile deps).
     * Needed so URLClassLoader can resolve JDA types referenced by the option type parameters.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val slashDefsClasspath: ConfigurableFileCollection

    /** Where to write generated `Abstract*CommandHandler.kt` files. */
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val outputDirectory = outputDir.get().asFile.also { it.mkdirs() }
        val classesDir = slashDefsClasses.get().asFile

        if (!classesDir.exists()) {
            logger.warn("[Slash] slashDefs classes directory does not exist: $classesDir — skipping")
            return
        }

        // ── 1. Build URLClassLoader ───────────────────────────────────────────
        val urls = (slashDefsClasspath.files + classesDir)
            .filter { it.exists() }
            .map { it.toURI().toURL() }
            .toTypedArray()

        val classLoader = URLClassLoader(urls, Thread.currentThread().contextClassLoader)

        try {
            // ── 2. Load all top-level Kt classes to trigger static initialisers ──
            classesDir.walkTopDown()
                .filter { it.isFile && it.extension == "class" && !it.name.contains("$") }
                .forEach { classFile ->
                    val className = classFile.relativeTo(classesDir).path
                        .replace('/', '.')
                        .replace('\\', '.')
                        .removeSuffix(".class")
                    try {
                        // forName with initialize=true triggers <clinit>, running top-level
                        // `val ping = command("ping") { ... }` initialisers that register commands.
                        Class.forName(className, true, classLoader)
                        logger.debug("[Slash] Loaded def class: $className")
                    } catch (e: Exception) {
                        logger.debug("[Slash] Could not load $className: ${e.message}")
                    }
                }

            // ── 3. Retrieve registered CommandDefs ────────────────────────────
            val registryClass = classLoader.loadClass("io.github.blad3mak3r.slash.dsl.CommandRegistry")
            val registryInstance = registryClass.getDeclaredField("INSTANCE").get(null)
            @Suppress("UNCHECKED_CAST")
            val commandDefs = registryClass.getMethod("getCommands").invoke(registryInstance) as List<*>

            if (commandDefs.isEmpty()) {
                logger.warn("[Slash] No CommandDef instances found in slashDefs source set.")
                return
            }

            // ── 4. Generate Abstract*CommandHandler.kt for each command ────────
            for (commandDef in commandDefs) {
                commandDef!!
                val cmdName = commandDef.javaClass.getMethod("getName").invoke(commandDef) as String
                val fileSpec = AbstractHandlerGenerator.generate(commandDef)
                val outputFile = outputDirectory.resolve("Abstract${cmdName.toPascalCase()}CommandHandler.kt")
                fileSpec.writeTo(outputDirectory)
                logger.lifecycle("[Slash] Generated ${outputFile.name}")
            }
        } finally {
            classLoader.close()
        }
    }

    private fun String.toPascalCase() =
        split("-", "_", " ").joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
}
