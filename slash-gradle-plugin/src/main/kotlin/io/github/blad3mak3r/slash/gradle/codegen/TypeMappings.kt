package io.github.blad3mak3r.slash.gradle.codegen

/**
 * Maps the Java class name of a DSL option type to the JDA OptionType and its accessor expression.
 */
object TypeMappings {

    data class Mapping(
        /** JDA OptionType enum constant name, e.g. "STRING" */
        val optionType: String,
        /** Expression applied to OptionMapping to get the value, e.g. ".asString()" */
        val accessor: String,
        /** Fully-qualified Java type returned by the accessor */
        val returnType: String
    )

    private val mappings: Map<String, Mapping> = mapOf(
        "java.lang.String"                            to Mapping("STRING",      ".asString()",   "java.lang.String"),
        "kotlin.String"                               to Mapping("STRING",      ".asString()",   "java.lang.String"),
        "int"                                         to Mapping("INTEGER",     ".asInt()",      "int"),
        "java.lang.Integer"                           to Mapping("INTEGER",     ".asInt()",      "java.lang.Integer"),
        "kotlin.Int"                                  to Mapping("INTEGER",     ".asInt()",      "java.lang.Integer"),
        "long"                                        to Mapping("INTEGER",     ".asLong()",     "long"),
        "java.lang.Long"                              to Mapping("INTEGER",     ".asLong()",     "java.lang.Long"),
        "kotlin.Long"                                 to Mapping("INTEGER",     ".asLong()",     "java.lang.Long"),
        "double"                                      to Mapping("NUMBER",      ".asDouble()",   "double"),
        "java.lang.Double"                            to Mapping("NUMBER",      ".asDouble()",   "java.lang.Double"),
        "kotlin.Double"                               to Mapping("NUMBER",      ".asDouble()",   "java.lang.Double"),
        "boolean"                                     to Mapping("BOOLEAN",     ".asBoolean()",  "boolean"),
        "java.lang.Boolean"                           to Mapping("BOOLEAN",     ".asBoolean()",  "java.lang.Boolean"),
        "kotlin.Boolean"                              to Mapping("BOOLEAN",     ".asBoolean()",  "java.lang.Boolean"),
        "net.dv8tion.jda.api.entities.User"           to Mapping("USER",        ".asUser()",     "net.dv8tion.jda.api.entities.User"),
        "net.dv8tion.jda.api.entities.Member"         to Mapping("USER",        ".asMember()",   "net.dv8tion.jda.api.entities.Member"),
        "net.dv8tion.jda.api.entities.Role"           to Mapping("ROLE",        ".asRole()",     "net.dv8tion.jda.api.entities.Role"),
        "net.dv8tion.jda.api.entities.IMentionable"   to Mapping("MENTIONABLE", ".asMentionable()", "net.dv8tion.jda.api.entities.IMentionable"),
        "net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion"
                                                      to Mapping("CHANNEL",     ".asChannel()",  "net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion"),
        "net.dv8tion.jda.api.entities.Message\$Attachment"
                                                      to Mapping("ATTACHMENT",  ".asAttachment()", "net.dv8tion.jda.api.entities.Message\$Attachment"),
    )

    fun get(javaClassName: String): Mapping =
        mappings[javaClassName]
            ?: error("Unsupported option type: '$javaClassName'. Supported: ${mappings.keys.sorted()}")

    fun optionTypeName(javaClassName: String) = get(javaClassName).optionType
    fun accessor(javaClassName: String) = get(javaClassName).accessor
    fun returnType(javaClassName: String) = get(javaClassName).returnType
}
