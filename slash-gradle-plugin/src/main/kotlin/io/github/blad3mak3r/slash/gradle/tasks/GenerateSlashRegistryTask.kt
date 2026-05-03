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
 * 1. Scans `build/classes/kotlin/main/` via [URLClassLoader] for concrete (non-abstract,
 *    non-interface) classes that implement any `*Command` interface from the
 *    `io.github.blad3mak3r.slash.generated` package.
 * 2. Generates `object SlashCommandRegistry : SlashRegistry` using KotlinPoet into [outputDir].
 *
 * The generated file is added to a `slashRegistry` source set so that it is compiled and
 * included in the final JAR.
 */
@CacheableTask
abstract class GenerateSlashRegistryTask : DefaultTask() {

    /** Compiled output of the `main` source set (user implementations + generated interfaces). */
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
            val slashHandlerClassName = "io.github.blad3mak3r.slash.SlashCommandHandler"
            val slashHandlerClass = try {
                classLoader.loadClass(slashHandlerClassName)
            } catch (e: ClassNotFoundException) {
                logger.error("[Slash] Cannot find SlashCommandHandler on classpath — is slash-runtime a dependency?")
                return
            }

            // ── 2. Load all classes, separate generated interfaces from concrete impls ──
            val generatedInterfaces = mutableListOf<Class<*>>()
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
                        if (!slashHandlerClass.isAssignableFrom(clazz) || clazz == slashHandlerClass) return@forEach

                        val isInterface = clazz.isInterface
                        val isGenerated = isInterface &&
                            clazz.name.startsWith("io.github.blad3mak3r.slash.generated.")
                        val isAbstract = java.lang.reflect.Modifier.isAbstract(clazz.modifiers)
                        val isConcrete = !isInterface && !isAbstract

                        when {
                            isGenerated -> {
                                generatedInterfaces += clazz
                                logger.debug("[Slash] Found generated interface: ${clazz.name}")
                            }
                            isConcrete -> {
                                concreteImpls += clazz
                                logger.debug("[Slash] Found concrete impl: ${clazz.name}")
                            }
                        }
                    } catch (e: Exception) {
                        logger.debug("[Slash] Could not load $className: ${e.message}")
                    }
                }

            if (concreteImpls.isEmpty()) {
                logger.warn("[Slash] No concrete SlashCommandHandler implementations found — registry will be empty")
            }

            // ── 3. Map generated interface → concrete impl ────────────────────
            // Each concrete impl should implement exactly one generated *Command interface.
            val implementations = mutableMapOf<String, String>()
            for (impl in concreteImpls) {
                val matchingInterface = generatedInterfaces.find { iface -> iface.isAssignableFrom(impl) }
                if (matchingInterface != null) {
                    val key = matchingInterface.simpleName
                    if (implementations.containsKey(key)) {
                        logger.warn(
                            "[Slash] Multiple implementations of ${matchingInterface.name}: " +
                            "${implementations[key]} and ${impl.name}. Using ${impl.name}."
                        )
                    }
                    implementations[key] = impl.name
                } else {
                    logger.warn("[Slash] Concrete handler ${impl.name} does not implement any known *Command interface — skipping")
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
