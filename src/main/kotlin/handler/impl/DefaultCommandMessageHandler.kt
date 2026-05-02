package org.example.handler.impl

import mu.KLogging
import org.example.dto.CommandDto
import org.example.dto.TextCommandEnum
import org.example.dto.TextCommandEnum.DOCKER_ACTIVE_SERVICES
import org.example.dto.TextCommandEnum.DOCKER_RESTART_SERVICE
import org.example.dto.TextCommandEnum.DOCKER_STOP_SERVICE
import org.example.dto.TextCommandEnum.SSH
import org.example.dto.TextCommandEnum.SSH_FAILED
import org.example.dto.TextCommandEnum.START
import org.example.dto.TextCommandEnum.STATUS
import org.example.dto.TextCommandEnum.UPTIME
import org.example.handler.CommandMessageHandler
import org.example.service.DockerMessageService
import org.example.service.SshMessageService
import org.example.service.SystemMessageService
import org.telegram.telegrambots.meta.api.objects.message.Message

class DefaultCommandMessageHandler(
    private val sshMessageService: SshMessageService,
    private val systemMessageService: SystemMessageService,
    private val dockerMessageService: DockerMessageService,
) : CommandMessageHandler {

    override fun handle(message: Message): String {
        val parsedCommand = parseCommand(message)

        val availableCommands = getAvailableCommands()

        if (parsedCommand == null) {
            return "Unknown command. Available: $availableCommands"
        }

        return when (parsedCommand.command) {
            START -> "Hi, it's server watchdog bot. Available commands: $availableCommands"
            STATUS -> systemMessageService.getSystemStatus()
            UPTIME -> systemMessageService.getUptime()
            SSH -> sshMessageService.getLastSuccessSshLogins()
            SSH_FAILED -> sshMessageService.getLastFailedSshLogins()
            DOCKER_ACTIVE_SERVICES -> dockerMessageService.getActiveDockerContainers()
            DOCKER_RESTART_SERVICE -> dockerMessageService.restartContainer(parsedCommand.arguments.firstOrNull())
            DOCKER_STOP_SERVICE -> dockerMessageService.stopContainer(parsedCommand.arguments.firstOrNull())
        }
    }

    private fun parseCommand(message: Message): CommandDto? {
        val text = message.text

        if (text == null || text.isBlank()) {
            logger.error { "Message text is blank" }
            return null
        }

        return try {
            val splitString = text.split(" ")

            val command = TextCommandEnum.valueOf(splitString.first().removePrefix("/").uppercase())

            CommandDto(
                command = command,
                arguments = splitString.drop(1),
            )
        } catch (e: Exception) {
            logger.error(e) { "Unknown command: $text" }
            null
        }
    }

    private fun getAvailableCommands(): String =
        TextCommandEnum.entries.joinToString(", ") { it.command }

    companion object : KLogging()
}