import org.junit.Test
import tv.blademaker.slash.api.BaseSlashCommand
import tv.blademaker.slash.api.SlashCommandContext
import tv.blademaker.slash.api.annotations.SlashCommandOption

class GenerateCommandTest {

    class CustomCommand : BaseSlashCommand("test") {

        @SlashCommandOption(group = "group1", name = "option1")
        fun group1Option1(ctx: SlashCommandContext) {

        }

        @SlashCommandOption(group = "group1", name = "option2")
        fun group1Option2(ctx: SlashCommandContext) {

        }

        @SlashCommandOption(group = "group2", name = "option1")
        fun group2Option1(ctx: SlashCommandContext) {

        }

        @SlashCommandOption(group = "group2", name = "option2")
        fun group2Option2(ctx: SlashCommandContext) {

        }

        @SlashCommandOption(name = "optionNoGroup")
        fun optionNoGroup(ctx: SlashCommandContext) {

        }

    }

    @Test
    fun `Generate command`() {
        val command = CustomCommand()

        val paths = command.paths

        val expected = listOf(
            "test",
            "test/optionNoGroup",
            "test/group1/option1",
            "test/group1/option2",
            "test/group2/option1",
            "test/group2/option2",
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