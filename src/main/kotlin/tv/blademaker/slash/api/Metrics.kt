package tv.blademaker.slash.api

import io.prometheus.client.Counter
import io.prometheus.client.Gauge
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent

internal object Metrics {

    fun register() {
        EXECUTED_COMMANDS.register<Counter>()
        SUCCESSFUL_EXECUTED_COMMANDS.register<Counter>()
        FAILED_EXECUTED_COMMANDS.register<Counter>()
    }

    fun incHandledCommand(event: SlashCommandEvent) {
        EXECUTED_COMMANDS.labels(event.commandPath).inc()
    }

    fun incSuccessCommand(event: SlashCommandEvent, measuredTime: Long) {
        SUCCESSFUL_EXECUTED_COMMANDS.labels(event.commandPath).inc()
        MEASURED_COMMAND_EXECUTION.labels(event.commandPath).set(measuredTime.toDouble())
    }

    fun incFailedCommand(event: SlashCommandEvent) {
        FAILED_EXECUTED_COMMANDS.labels(event.commandPath).inc()
    }

    private val EXECUTED_COMMANDS: Counter = Counter.build()
        .name("slash_command_counter")
        .help("Slash commands handled.")
        .labelNames("pathName")
        .create()

    private val SUCCESSFUL_EXECUTED_COMMANDS: Counter = Counter.build()
        .name("slash_commands_success")
        .help("Slash commands handled that ends successfully.")
        .labelNames("pathName")
        .create()

    private val FAILED_EXECUTED_COMMANDS: Counter = Counter.build()
        .name("slash_commands_failed")
        .help("Slash commands handled that ends on a Exception.")
        .labelNames("pathName")
        .create()

    private val MEASURED_COMMAND_EXECUTION: Gauge = Gauge.build()
        .name("slash_measured_execution_time")
        .help("The time in milliseconds require to execute the command.")
        .labelNames("pathName")
        .create()
}