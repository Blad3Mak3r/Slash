@file:Suppress("UNUSED_PARAMETER", "unused")

import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.events.GenericEvent
import tv.blademaker.slash.BaseSlashCommand
import tv.blademaker.slash.annotations.InteractionTarget
import tv.blademaker.slash.annotations.OnAutoComplete
import tv.blademaker.slash.annotations.OnSlashCommand
import tv.blademaker.slash.annotations.OptionName
import tv.blademaker.slash.client.SlashCommandClient
import tv.blademaker.slash.context.AutoCompleteContext
import tv.blademaker.slash.context.SlashCommandContext
import tv.blademaker.slash.exceptions.ExceptionHandler
import tv.blademaker.slash.exceptions.ExceptionHandlerImpl
import tv.blademaker.slash.internal.Registry

class GenerateCommandTest {

    object DummySlashCommandClient : SlashCommandClient {
        override val registry = Registry(
            slash = listOf(
                BasicCommand,
                AdvancedCommand
            ),
            user = emptyList(),
            message = emptyList()
        )
        override val exceptionHandler: ExceptionHandler = ExceptionHandlerImpl()
        override fun onEvent(event: GenericEvent) {
        }
    }

    object BasicCommand : BaseSlashCommand("basic") {

        @OnSlashCommand(target = InteractionTarget.ALL)
        fun handle(ctx: SlashCommandContext) {

        }

        @OnAutoComplete(optionName = "name")
        fun handleNext(ctx: AutoCompleteContext, @OptionName("name") option: String) {
        }

    }

    @Suppress("UNUSED_PARAMETER", "unused")
    object AdvancedCommand : BaseSlashCommand("advanced") {

        @OnSlashCommand(group = "group1", name = "option1", target = InteractionTarget.GUILD)
        fun group1Option1(ctx: SlashCommandContext, @OptionName("channel") voiceChannel: VoiceChannel?) {
            // We are using @OptionName with custom name channel
            // this means voiceChannel will be equal to ctx.getOption("channel")
        }

        @OnSlashCommand(group = "group1", name = "option2", target = InteractionTarget.ALL)
        fun group1Option2(ctx: SlashCommandContext) {

        }

        @OnSlashCommand(group = "group2", name = "option1", target = InteractionTarget.ALL)
        fun group2Option1(ctx: SlashCommandContext) {

        }

        @OnSlashCommand(group = "group2", name = "option2", target = InteractionTarget.ALL)
        fun group2Option2(ctx: SlashCommandContext) {

        }

        @OnSlashCommand(name = "optionNoGroup", target = InteractionTarget.ALL)
        fun optionNoGroup(ctx: SlashCommandContext) {

        }

    }

    /*@Test
    fun `Test basic command`() {
        val command = DummySlashCommandClient.getCommand("basic")!!

        val paths = DummySlashCommandClient.find

        val expected = listOf(
            "basic"
        ).sorted()

        println("Command generated paths:")
        println(paths)
        println()
        println("Expected paths:")
        println(expected)
        println()

        assert(paths == expected) {
            "Arrays are not equals"
        }
    }

    @Test
    fun `Test advanced command`() {
        val command = DummySlashCommandClient.getCommand("advanced")!!

        val paths = command.paths

        val expected = listOf(
            "advanced/optionNoGroup",
            "advanced/group1/option1",
            "advanced/group1/option2",
            "advanced/group2/option1",
            "advanced/group2/option2",
        ).sorted()

        println("Command generated paths:")
        println(paths)
        println()
        println("Expected paths:")
        println(expected)
        println()

        assert(paths == expected) {
            "Arrays are not equals"
        }
    }*/

}