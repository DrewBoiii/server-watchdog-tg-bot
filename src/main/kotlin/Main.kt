package org.example

import okhttp3.OkHttpClient
import org.example.dto.BOT_TOKEN_ENV_VARIABLE
import org.example.handler.impl.DefaultCommandMessageHandler
import org.example.sender.impl.DefaultMessageSender
import org.example.service.DockerMessageService
import org.example.service.DockerService
import org.example.service.SshMessageService
import org.example.service.SystemMessageService
import org.example.service.UbuntuSshService
import org.newsclub.net.unix.AFSocketFactory
import org.newsclub.net.unix.AFUNIXSocketAddress
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import java.nio.file.Path

fun main() {
    val botsApplication = TelegramBotsLongPollingApplication()
    try {
        val botToken = getBotToken()
        botsApplication.registerBot(
            botToken,
            initServerWatchdog(botToken),
        )
        println("Watchdog bot started...")
        Thread.currentThread().join()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun getBotToken(): String =
    System.getenv(BOT_TOKEN_ENV_VARIABLE) ?: throw IllegalStateException("$BOT_TOKEN_ENV_VARIABLE is not set")

fun initServerWatchdog(botToken: String) =
    ServerWatchdog(
        messageSender = DefaultMessageSender(
            telegramClient = OkHttpTelegramClient(botToken),
        ),
        commandMessageHandler = DefaultCommandMessageHandler(
            sshMessageService = SshMessageService(UbuntuSshService()),
            systemMessageService = SystemMessageService(),
            dockerMessageService = DockerMessageService(
                dockerService = DockerService(
                    dockerHttpClient = OkHttpClient.Builder()
                        .socketFactory(
                            socketFactory = AFSocketFactory.FixedAddressSocketFactory(
                                AFUNIXSocketAddress.of(Path.of("/var/run/docker.sock"))
                            )
                        )
                        .build(),
                ),
            ),
        ),
    )