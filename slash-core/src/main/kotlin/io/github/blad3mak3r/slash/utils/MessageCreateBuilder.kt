package io.github.blad3mak3r.slash.utils

import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder

fun MessageCreateBuilder.appendLine(): MessageCreateBuilder {
    this.addContent("\n")
    return this
}

fun MessageCreateBuilder.appendLine(content: String): MessageCreateBuilder {
    this.addContent("$content\n")
    return this
}

fun MessageCreateBuilder.append(content: String): MessageCreateBuilder {
    this.addContent(content)
    return this
}

fun MessageCreateBuilder.appendCodeBlock(content: String, language: String = ""): MessageCreateBuilder {
    this.addContent("\n```$language\n$content\n```\n")
    return this
}