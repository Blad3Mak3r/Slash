package io.github.blad3mak3r.slash.metrics

import io.prometheus.client.Counter
import io.prometheus.client.Gauge
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import tv.blademaker.slash.extensions.commandPath

internal class Metrics(strategy: MetricsStrategy) {

    private val executedCommandsCounter: Counter = Counter.build()
        .name("${strategy.baseName}_command_counter")
        .help("Slash commands handled.")
        .labelNames("pathName")
        .create()

    private val successfulCommandsCounter: Counter = Counter.build()
        .name("${strategy.baseName}_commands_success")
        .help("Slash commands handled that ends successfully.")
        .labelNames("pathName")
        .create()

    private val failedCommandsCounter: Counter = Counter.build()
        .name("${strategy.baseName}_commands_failed")
        .help("Slash commands handled that ends on a Exception.")
        .labelNames("pathName")
        .create()

    private val measureTimeGauge: Gauge = Gauge.build()
        .name("${strategy.baseName}_measured_execution_time")
        .help("The time in milliseconds require to execute the command.")
        .labelNames("pathName")
        .create()

    init {
        if (strategy.executedCommands) executedCommandsCounter.register<Counter>()
        if (strategy.successfulCommands) successfulCommandsCounter.register<Counter>()
        if (strategy.failedCommands) failedCommandsCounter.register<Counter>()
        if (strategy.measureTime) measureTimeGauge.register<Gauge>()
    }

    fun incHandledCommand(event: SlashCommandInteractionEvent) {
        executedCommandsCounter.labels(event.commandPath).inc()
    }

    fun incSuccessCommand(event: SlashCommandInteractionEvent, measuredTime: Long) {
        successfulCommandsCounter.labels(event.commandPath).inc()
        measureTimeGauge.labels(event.commandPath).set(measuredTime.toDouble())
    }

    fun incFailedCommand(event: SlashCommandInteractionEvent) {
        failedCommandsCounter.labels(event.commandPath).inc()
    }
}