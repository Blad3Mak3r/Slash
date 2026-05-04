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
    fun accessorFor(typeName: String): String? = when (typeName) {
        "String"                                                -> "asString()"
        "Long"                                                  -> "asLong()"
        "Int"                                                   -> "asString().toInt()"
        "Boolean"                                               -> "asBoolean()"
        "Double"                                                -> "asDouble()"
        "Float"                                                 -> "asDouble().toFloat()"
        "Member",
        "net.dv8tion.jda.api.entities.Member"                   -> "asMember!!"
        "User",
        "net.dv8tion.jda.api.entities.User"                     -> "asUser()"
        "Role",
        "net.dv8tion.jda.api.entities.Role"                     -> "asRole()"
        // KSP reports simpleName as "Attachment" for the nested class Message.Attachment
        "Attachment",
        "Message.Attachment",
        "net.dv8tion.jda.api.entities.Message.Attachment"       -> "asAttachment()"
        "IMentionable",
        "net.dv8tion.jda.api.entities.IMentionable"             -> "asMentionable()"
        // Channels — use asChannel() with a safe cast and require non-null at the call site
        "GuildChannel",
        "net.dv8tion.jda.api.entities.channel.middleman.GuildChannel"
                                                                -> "asChannel()"
        "TextChannel",
        "net.dv8tion.jda.api.entities.channel.concrete.TextChannel"
                                                                -> "asChannel() as? net.dv8tion.jda.api.entities.channel.concrete.TextChannel"
        "VoiceChannel",
        "net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel"
                                                                -> "asChannel() as? net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel"
        else                                                    -> null
    }

    fun isSupported(typeName: String): Boolean = accessorFor(typeName) != null
}
