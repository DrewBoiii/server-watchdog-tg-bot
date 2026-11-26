package org.example

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.example.alert.AlertDispatcher
import org.example.alert.SshLogAlertSource
import org.example.alert.TelegramAlertNotifier
import org.example.config.Config
import org.example.dto.ALERT_CHAT_ID_ENV_VARIABLE
import org.example.dto.ALLOWED_USER_IDS_ENV_VARIABLE
import org.example.dto.BOT_TOKEN_ENV_VARIABLE
import org.example.factory.AuthLogParserFactory
import org.example.factory.DockerHttpClientFactory
import org.example.handler.impl.DefaultCommandMessageHandler
import org.example.sender.impl.DefaultMessageSender
import org.example.service.DockerMessageService
import org.example.service.DockerService
import org.example.service.JvmMessageService
import org.example.service.SshMessageService
import org.example.service.SystemMessageService
import org.example.service.UbuntuSshService
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import java.io.File
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

        val alertChatId = resolveAlertChatId()
        val alertMessageSender = DefaultMessageSender(
            telegramClient = OkHttpTelegramClient(botToken),
        )
        val alertNotifier = TelegramAlertNotifier(alertMessageSender, alertChatId)
        val alertDispatcher = AlertDispatcher(
            sources = listOf(
                SshLogAlertSource(
                    authLogPath = Config.config.system.sshAuthLogFilePath,
                    cooldownLoginWindowInMillis = 5_000L,
                    tailReadingRestartCooldownInMillis = 5_000L,
                ),
            ),
            scope = botScope,
        )

        botScope.launch {
            alertDispatcher.events.collect { event ->
                try {
                    alertNotifier.notify(event)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        alertDispatcher.start()

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

fun resolveAlertChatId(): Long {
    System.getenv(ALERT_CHAT_ID_ENV_VARIABLE)?.toLongOrNull()?.let { return it }

    val allowedChatId = System.getenv(ALLOWED_USER_IDS_ENV_VARIABLE)
        ?.split(",")
        ?.mapNotNull { it.trim().toLongOrNull() }
        .orEmpty()

    return allowedChatId.firstOrNull()
        ?: throw IllegalStateException("Set $ALERT_CHAT_ID_ENV_VARIABLE or at least one $ALLOWED_USER_IDS_ENV_VARIABLE")
}

fun initServerWatchdog(botToken: String, coroutineScope: CoroutineScope) = Config.config.let { config ->
    ServerWatchdog(
        messageSender = DefaultMessageSender(
            telegramClient = OkHttpTelegramClient(botToken),
        ),
        commandMessageHandler = DefaultCommandMessageHandler(
            sshMessageService = SshMessageService(
                sshService = UbuntuSshService(
                    sshLogFile = File(config.system.sshAuthLogFilePath)
                ),
                authLogParsers = AuthLogParserFactory().createAuthLogParsers(),
            ),
            jvmMessageService = JvmMessageService(),
            systemMessageService = SystemMessageService(
                uptimeFile = File(config.system.uptimeFilePath),
                memoryInfoFile = File(config.system.ramInfoFilePath),
                loadAverageFile = File(config.system.loadAverageFilePath),
            ),
            dockerMessageService = DockerMessageService(
                dockerService = DockerService(
                    dockerHttpClient = DockerHttpClientFactory(config).createHttpClient(),
                ),
            ),
        ),
        coroutineScope = coroutineScope,
    )
}