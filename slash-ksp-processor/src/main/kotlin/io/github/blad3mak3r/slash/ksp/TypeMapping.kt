package io.github.blad3mak3r.slash.ksp

/**
 * Maps Kotlin parameter types to the correct JDA OptionMapping accessor code.
 *
 * The key is the simple class name (or fully-qualified for JDA types).
 */
object TypeMapping {

    /**
     * Returns the accessor expression for a non-null option, e.g. `asString()`.
     * Returns `null` if the type is unsupported.
     */
    /**
     * Returns the accessor expression for an option.
     *
     * For concrete channel subtypes (TextChannel, VoiceChannel) the cast
     * operator depends on the parameter's nullability:
     *  - non-null param → `as TextChannel`  (ClassCastException if type mismatch)
     *  - nullable param → `as? TextChannel` (returns null if type mismatch)
     *
     * Returns `null` if the type is unsupported.
     */
    fun accessorFor(typeName: String, nullable: Boolean = false): String? = when (typeName) {
        // JDA 6 exposes all OptionMapping getters as Kotlin properties (getAsXxx → asXxx).
        // Access them without parentheses; Kotlin extension functions on the value still use ().
        "String"                                                -> "asString"
        "Long"                                                  -> "asLong"
        "Int"                                                   -> "asString.toInt()"
        "Boolean"                                               -> "asBoolean"
        "Double"                                                -> "asDouble"
        "Float"                                                 -> "asDouble.toFloat()"
        "Member",
        "net.dv8tion.jda.api.entities.Member"                   -> "asMember!!"
        "User",
        "net.dv8tion.jda.api.entities.User"                     -> "asUser"
        "Role",
        "net.dv8tion.jda.api.entities.Role"                     -> "asRole"
        // KSP reports simpleName as "Attachment" for the nested class Message.Attachment
        "Attachment",
        "Message.Attachment",
        "net.dv8tion.jda.api.entities.Message.Attachment"       -> "asAttachment"
        "IMentionable",
        "net.dv8tion.jda.api.entities.IMentionable"             -> "asMentionable"
        // GuildChannel: asChannel returns GuildChannelUnion which implements GuildChannel — no cast needed
        "GuildChannel",
        "net.dv8tion.jda.api.entities.channel.middleman.GuildChannel"
                                                                -> "asChannel"
        // Concrete channel subtypes require a cast; use safe/unsafe depending on nullability
        "TextChannel",
        "net.dv8tion.jda.api.entities.channel.concrete.TextChannel" ->
            if (nullable) "asChannel as? net.dv8tion.jda.api.entities.channel.concrete.TextChannel"
            else          "asChannel as net.dv8tion.jda.api.entities.channel.concrete.TextChannel"
        "VoiceChannel",
        "net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel" ->
            if (nullable) "asChannel as? net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel"
            else          "asChannel as net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel"
        else                                                    -> null
    }

    fun isSupported(typeName: String): Boolean = accessorFor(typeName) != null
}
