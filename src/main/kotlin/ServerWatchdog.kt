package org.example

import mu.KLogging
import org.example.handler.CommandHandler
import org.example.handler.impl.DefaultCommandHandler
import org.example.sender.impl.DefaultMessageSender
import org.example.sender.MessageSender
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.objects.Update

class ServerWatchdog : LongPollingSingleThreadUpdateConsumer {

    private val messageSender: MessageSender = DefaultMessageSender()

    private val commandHandler: CommandHandler = DefaultCommandHandler()

    private val allowedUserIds: Set<Long> = System.getenv(ALLOWED_USER_IDS_ENV_VARIABLE)
        ?.split(USER_IDS_DELIMITER)
        ?.mapNotNull { it.trim().toLongOrNull() }
        ?.toSet()
        ?: throw IllegalStateException("$ALLOWED_USER_IDS_ENV_VARIABLE environment variable is not set")

    override fun consume(update: Update?) {
        update?.let {
            val senderId = update.message?.from?.id ?: "Unknown user"

            if (senderId !in allowedUserIds) {
                logger.warn { "Message from unknown user: $senderId." }
                return
            }

            if (update.hasMessage() && senderId in allowedUserIds) {
                val message = update.message

                val response = commandHandler.handle(message)

                messageSender.sendMessage(message.chatId, response)
            }
        }
    }

    companion object : KLogging() {
        const val ALLOWED_USER_IDS_ENV_VARIABLE = "ALLOWED_USER_IDS"
        const val USER_IDS_DELIMITER = ","
    }
}