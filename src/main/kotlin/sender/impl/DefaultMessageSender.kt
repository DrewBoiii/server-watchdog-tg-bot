package org.example.sender.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.sender.MessageSender
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.generics.TelegramClient

class DefaultMessageSender(
    private val telegramClient: TelegramClient,
) : MessageSender {

    override suspend fun sendMessage(chatId: Long, text: String) {
        val message = SendMessage(chatId.toString(), text)
            .apply { disableWebPagePreview() }
        withContext(Dispatchers.IO) {
            telegramClient.execute(message)
        }
    }

}