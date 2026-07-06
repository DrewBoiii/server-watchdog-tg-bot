package org.example

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.example.config.Config
import org.example.dto.BOT_TOKEN_ENV_VARIABLE
import org.example.handler.impl.DefaultCommandMessageHandler
import org.example.sender.impl.DefaultMessageSender
import org.example.service.DockerMessageService
import org.example.service.DockerService
import org.example.service.JvmMessageService
import org.example.service.SshMessageService
import org.example.service.SystemMessageService
import org.example.service.UbuntuSshService
import org.newsclub.net.unix.AFSocketFactory
import org.newsclub.net.unix.AFUNIXSocketAddress
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import java.io.File
import java.nio.file.Path
import java.util.concurrent.Executors

fun main() = runBlocking {
    val botScope = CoroutineScope(
        Executors.newSingleThreadExecutor().asCoroutineDispatcher() + CoroutineExceptionHandler { _, e ->
            e.printStackTrace()
        }
    )

    val botsApplication = TelegramBotsLongPollingApplication()
    try {
        val botToken = getBotToken()
        val bot = initServerWatchdog(botToken, botScope)
        val botSession = botsApplication.registerBot(botToken, bot)

        Runtime.getRuntime().addShutdownHook(Thread {
            runBlocking {
                botScope.cancel()
                botSession.stop()
            }
        })

        println("Watchdog bot started...")
        awaitCancellation()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        botScope.cancel()
    }
}

fun getBotToken(): String =
    System.getenv(BOT_TOKEN_ENV_VARIABLE) ?: throw IllegalStateException("$BOT_TOKEN_ENV_VARIABLE is not set")

fun initServerWatchdog(botToken: String, coroutineScope: CoroutineScope) =
    ServerWatchdog(
        messageSender = DefaultMessageSender(
            telegramClient = OkHttpTelegramClient(botToken),
        ),
        commandMessageHandler = DefaultCommandMessageHandler(
            sshMessageService = SshMessageService(
                sshService = UbuntuSshService(
                    sshLogFile = File("/var/log/auth.log")
                )
            ),
            jvmMessageService = JvmMessageService(),
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
                    applicationConfig = Config.config,
                ),
            ),
        ),
        coroutineScope = coroutineScope,
    )