import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import org.junit.Test
import tv.blademaker.slash.api.BaseSlashCommand
import tv.blademaker.slash.api.DefaultSlashCommandClient
import tv.blademaker.slash.api.SlashCommandClient
import tv.blademaker.slash.api.SlashCommandContext
import tv.blademaker.slash.api.annotations.OptionName
import tv.blademaker.slash.api.annotations.SlashCommand

class GenerateCommandTest {

    object DummySlashCommandClient : SlashCommandClient {
        override val registry: List<BaseSlashCommand>
            get() = listOf(
                BasicCommand,
                AdvancedCommand
            )

        override fun onSlashCommandEvent(event: SlashCommandEvent) {
        }
    }

    object BasicCommand : BaseSlashCommand("basic") {

        @SlashCommand
        fun handle(ctx: SlashCommandContext) {

        }

    }

    @Suppress("UNUSED_PARAMETER", "unused")
    object AdvancedCommand : BaseSlashCommand("advanced") {

        @SlashCommand(group = "group1", name = "option1")
        fun group1Option1(ctx: SlashCommandContext, @OptionName("channel") voiceChannel: VoiceChannel?) {
            // We are using @OptionName with custom name channel
            // this means voiceChannel will be equal to ctx.getOption("channel")
        }

        @SlashCommand(group = "group1", name = "option2")
        fun group1Option2(ctx: SlashCommandContext) {

        }

        @SlashCommand(group = "group2", name = "option1")
        fun group2Option1(ctx: SlashCommandContext) {

        }

        @SlashCommand(group = "group2", name = "option2")
        fun group2Option2(ctx: SlashCommandContext) {

        }

        @SlashCommand(name = "optionNoGroup")
        fun optionNoGroup(ctx: SlashCommandContext) {

        }

    }

    @Test
    fun `Test basic command`() {
        val command = DummySlashCommandClient.getCommand("basic")!!

        val paths = command.paths

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
    }

}