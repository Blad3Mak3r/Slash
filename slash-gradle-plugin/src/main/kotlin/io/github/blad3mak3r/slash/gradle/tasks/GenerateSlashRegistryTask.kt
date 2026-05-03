package io.github.blad3mak3r.slash.gradle.tasks

import io.github.blad3mak3r.slash.gradle.codegen.RegistryGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*
import java.net.URLClassLoader

/**
 * **Pass 2** — Registry generation.
 *
 * After `compileKotlin` has built the user's implementation classes, this task:
 * 1. Scans `build/classes/kotlin/main/` via [URLClassLoader] for concrete (non-abstract)
 *    subclasses of any `Abstract*CommandHandler` that is present on the classpath.
 * 2. Generates `object SlashCommandRegistry : SlashRegistry` using KotlinPoet into [outputDir].
 *
 * The generated file is added to a `slashRegistry` source set so that it is compiled and
 * included in the final JAR.
 */
@CacheableTask
abstract class GenerateSlashRegistryTask : DefaultTask() {

    /** Compiled output of the `main` source set (user implementations + generated abstract handlers). */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val mainClassesDir: DirectoryProperty

    /** Full compile classpath (needed to load JDA + slash-runtime types when inspecting classes). */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val compileClasspath: ConfigurableFileCollection

    /** Where to write the generated `SlashCommandRegistry.kt`. */
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val outputDirectory = outputDir.get().asFile.also { it.mkdirs() }
        val classesDir = mainClassesDir.get().asFile

        if (!classesDir.exists()) {
            logger.warn("[Slash] Main classes directory does not exist: $classesDir — skipping registry generation")
            return
        }

        // ── 1. Build URLClassLoader ───────────────────────────────────────────
        val urls = (compileClasspath.files + classesDir)
            .filter { it.exists() }
            .map { it.toURI().toURL() }
            .toTypedArray()

        val classLoader = URLClassLoader(urls, Thread.currentThread().contextClassLoader)

        try {
            val abstractHandlerClassName = "io.github.blad3mak3r.slash.AbstractCommandHandler"
            val abstractHandlerClass = try {
                classLoader.loadClass(abstractHandlerClassName)
            } catch (e: ClassNotFoundException) {
                logger.error("[Slash] Cannot find AbstractCommandHandler on classpath — is slash-runtime a dependency?")
                return
            }

            // ── 2. Load all classes, find abstract handlers + concrete impls ──
            val abstractHandlers = mutableListOf<Class<*>>()
            val concreteImpls = mutableListOf<Class<*>>()

            classesDir.walkTopDown()
                .filter { it.isFile && it.extension == "class" && !it.name.contains("$") }
                .forEach { classFile ->
                    val className = classFile.relativeTo(classesDir).path
                        .replace('/', '.')
                        .replace('\\', '.')
                        .removeSuffix(".class")
                    try {
                        val clazz = classLoader.loadClass(className)
                        if (abstractHandlerClass.isAssignableFrom(clazz) && clazz != abstractHandlerClass) {
                            val isAbstract = java.lang.reflect.Modifier.isAbstract(clazz.modifiers)
                            val isGenerated = clazz.simpleName.startsWith("Abstract")
                            if (isAbstract || isGenerated) {
                                abstractHandlers += clazz
                                logger.debug("[Slash] Found abstract handler: ${clazz.name}")
                            } else {
                                concreteImpls += clazz
                                logger.debug("[Slash] Found concrete impl: ${clazz.name}")
                            }
                        }
                    } catch (e: Exception) {
                        logger.debug("[Slash] Could not load $className: ${e.message}")
                    }
                }

            if (concreteImpls.isEmpty()) {
                logger.warn("[Slash] No concrete AbstractCommandHandler implementations found — registry will be empty")
            }

            // ── 3. Map abstract handler → concrete impl ───────────────────────
            // Each concrete impl should extend exactly one Abstract*CommandHandler
            val implementations = mutableMapOf<String, String>()
            for (impl in concreteImpls) {
                val matchingAbstract = abstractHandlers.find { abs ->
                    abs.isAssignableFrom(impl) && abs != abstractHandlerClass
                }
                if (matchingAbstract != null) {
                    val key = matchingAbstract.simpleName
                    if (implementations.containsKey(key)) {
                        logger.warn(
                            "[Slash] Multiple implementations of ${matchingAbstract.name}: " +
                            "${implementations[key]} and ${impl.name}. Using ${impl.name}."
                        )
                    }
                    implementations[key] = impl.name
                } else {
                    logger.warn("[Slash] Concrete handler ${impl.name} does not extend any known Abstract*CommandHandler — skipping")
                }
            }

            // ── 4. Generate SlashCommandRegistry.kt ───────────────────────────
            val fileSpec = RegistryGenerator.generate(implementations)
            fileSpec.writeTo(outputDirectory)
            logger.lifecycle("[Slash] Generated SlashCommandRegistry with ${implementations.size} handler(s)")

        } finally {
            classLoader.close()
        }
    }
}
