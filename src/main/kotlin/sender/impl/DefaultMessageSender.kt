package org.example.sender.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KLogging
import org.example.sender.MessageSender
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.generics.TelegramClient

class DefaultMessageSender(
    private val telegramClient: TelegramClient,
) : MessageSender {

    override suspend fun sendMessage(chatId: Long, text: String) {
        val message = SendMessage(chatId.toString(), text)
            .apply { disableWebPagePreview() }
        try {
            withContext(Dispatchers.IO) {
                telegramClient.execute(message)
            }
        } catch (e: TelegramApiException) {
            logger.error(e) { "Error sending message: ${e.localizedMessage}" }
        } catch (e: Exception) {
            logger.error(e) { "Unknown error during send message: ${e.localizedMessage}" }
        }
    }

    companion object : KLogging()
}