package io.github.blad3mak3r.slash.gradle.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

/**
 * Generates the `object SlashCommandRegistry : SlashRegistry` file by scanning compiled
 * classes from the main source set for concrete (non-abstract) subclasses of each
 * `Abstract*CommandHandler`.
 */
object RegistryGenerator {

    private val SLASH_REGISTRY =
        ClassName("io.github.blad3mak3r.slash", "SlashRegistry")
    private val ABSTRACT_COMMAND_HANDLER =
        ClassName("io.github.blad3mak3r.slash", "AbstractCommandHandler")

    /**
     * @param implementations Map of abstract handler class name (simple) → concrete class FQN.
     *        e.g. "AbstractBanCommandHandler" → "com.example.BanCommandImpl"
     */
    fun generate(implementations: Map<String, String>): FileSpec {
        val listType = LIST.parameterizedBy(ABSTRACT_COMMAND_HANDLER)

        val handlersInit = CodeBlock.builder()
            .add("listOf(\n")
        for ((_, implFqn) in implementations) {
            val implClass = ClassName.bestGuess(implFqn)
            handlersInit.add("    %T(),\n", implClass)
        }
        handlersInit.add(")")

        val handlersProperty = PropertySpec.builder("handlers", listType)
            .addModifiers(KModifier.OVERRIDE)
            .initializer(handlersInit.build())
            .build()

        val objectSpec = TypeSpec.objectBuilder("SlashCommandRegistry")
            .addSuperinterface(SLASH_REGISTRY)
            .addProperty(handlersProperty)
            .build()

        return FileSpec.builder("io.github.blad3mak3r.slash.generated", "SlashCommandRegistry")
            .addType(objectSpec)
            .build()
    }
}
