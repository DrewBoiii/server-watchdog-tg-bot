package org.example.sender.impl

import mu.KLogging
import org.example.sender.MessageSender
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.generics.TelegramClient

class DefaultMessageSender : MessageSender {

    private val botToken: String = System.getenv(BOT_TOKEN_ENV_VARIABLE)
        ?: throw IllegalStateException("$BOT_TOKEN_ENV_VARIABLE environment variable is not set")

    private val telegramClient: TelegramClient = OkHttpTelegramClient(botToken)

    override fun sendMessage(chatId: Long, text: String) {
        val message = SendMessage(chatId.toString(), text)
        try {
            telegramClient.execute(message)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
            logger.error(e) { "Error sending message: ${e.localizedMessage}" }
        }
    }

    companion object : KLogging() {
        const val BOT_TOKEN_ENV_VARIABLE = "BOT_TOKEN"
    }
}