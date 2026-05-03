package io.github.blad3mak3r.slash.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProcessSlashDefsTaskFunctionalTest {

    @get:Rule
    val testProjectDir = TemporaryFolder()

    // ── shared project setup ──────────────────────────────────────────────────

    private fun setupProject(commandsKt: String) {
        // Minimal settings — no external plugin repo needed because TestKit injects
        // both our plugin and kotlin-gradle-plugin via pluginUnderTestMetadata.
        testProjectDir.newFile("settings.gradle.kts").writeText(
            "rootProject.name = \"slash-test\""
        )

        // SlashPlugin itself applies kotlin("jvm") internally, so we don't declare it
        // here — that avoids a duplicate Kotlin plugin classloader in TestKit.
        testProjectDir.newFile("build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.blad3mak3r.slash")
            }
            repositories { mavenCentral() }
            """.trimIndent()
        )

        val slashDir = testProjectDir.newFolder("src", "slash", "kotlin")
        File(slashDir, "Commands.kt").writeText(commandsKt)
    }

    private fun runner(vararg args: String) =
        GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments(*args, "--stacktrace")
            .withPluginClasspath()
            .forwardOutput()

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    fun `processSlashDefs generates PingCommand interface for simple command`() {
        setupProject(
            """
            import io.github.blad3mak3r.slash.dsl.command

            val ping = command("ping") {
                description = "Ping the bot"
                option<String>("message") {
                    description = "Your message"
                    required()
                }
            }
            """.trimIndent()
        )

        val result = runner("processSlashDefs").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":processSlashDefs")!!.outcome)

        val generatedFile = findGeneratedFile("PingCommand.kt")
        assertNotNull("PingCommand.kt must be generated", generatedFile)

        val code = generatedFile!!.readText()
        assertTrue("interface PingCommand", code.contains("interface PingCommand"))
        assertTrue("extends SlashCommandHandler", code.contains(": SlashCommandHandler"))
        assertTrue("onPing method", code.contains("fun onPing("))
        assertTrue("message parameter", code.contains("message"))
        assertTrue("buildCommandData present", code.contains("buildCommandData"))
        assertTrue("slash name", code.contains("\"ping\""))
        assertTrue("KDoc path comment", code.contains("/ping"))
    }

    @Test
    fun `processSlashDefs generates BanCommand interface with subcommands and buttons`() {
        setupProject(
            """
            import io.github.blad3mak3r.slash.dsl.command

            val ban = command("ban") {
                description = "Ban management"
                guildOnly()
                subcommand("member") {
                    description = "Ban a member"
                    option<String>("reason") {
                        description = "Reason for ban"
                    }
                }
                button("ban-confirm-abc")
            }
            """.trimIndent()
        )

        val result = runner("processSlashDefs").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":processSlashDefs")!!.outcome)

        val generatedFile = findGeneratedFile("BanCommand.kt")
        assertNotNull("BanCommand.kt must be generated", generatedFile)

        val code = generatedFile!!.readText()
        assertTrue("interface BanCommand", code.contains("interface BanCommand"))
        assertTrue("onMember method", code.contains("fun onMember("))
        assertTrue("dispatchButton present", code.contains("dispatchButton"))
        assertTrue("BTN_REGEX constant", code.contains("BTN_REGEX"))
        assertTrue("KDoc path /ban member", code.contains("/ban member"))
    }

    @Test
    fun `processSlashDefs task is UP-TO-DATE on second run`() {
        setupProject(
            """
            import io.github.blad3mak3r.slash.dsl.command
            val ping = command("ping") { description = "Ping" }
            """.trimIndent()
        )

        runner("processSlashDefs").build()
        val result = runner("processSlashDefs").build()

        assertEquals(
            "task should be UP-TO-DATE on second run",
            TaskOutcome.UP_TO_DATE,
            result.task(":processSlashDefs")!!.outcome
        )
    }

    @Test
    fun `processSlashDefs generates multiple interfaces when multiple commands defined`() {
        setupProject(
            """
            import io.github.blad3mak3r.slash.dsl.command

            val ping = command("ping") { description = "Ping" }
            val ban  = command("ban")  { description = "Ban" }
            """.trimIndent()
        )

        val result = runner("processSlashDefs").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":processSlashDefs")!!.outcome)

        assertNotNull("PingCommand.kt", findGeneratedFile("PingCommand.kt"))
        assertNotNull("BanCommand.kt",  findGeneratedFile("BanCommand.kt"))
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun findGeneratedFile(name: String): File? =
        File(testProjectDir.root, "build/generated/slash/handlers")
            .walkTopDown()
            .firstOrNull { it.isFile && it.name == name }
}
