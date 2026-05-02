package org.example

import mu.KLogging
import org.example.dto.ALLOWED_USER_IDS_ENV_VARIABLE
import org.example.handler.CommandMessageHandler
import org.example.handler.impl.DefaultCommandMessageHandler
import org.example.sender.MessageSender
import org.example.sender.impl.DefaultMessageSender
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.objects.Update

class ServerWatchdog(
    private val messageSender: MessageSender,
    private val commandMessageHandler: CommandMessageHandler,
) : LongPollingSingleThreadUpdateConsumer {

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

                update.message?.let {
                    messageSender.sendMessage(
                        it.chatId,
                        "You are not allowed to message to this bot, please contact developer."
                    )
                }

                return
            }

            if (update.hasMessage() && senderId in allowedUserIds) {
                val message = update.message

                val response = commandMessageHandler.handle(message)

                messageSender.sendMessage(message.chatId, response)
            }
        }
    }

    companion object : KLogging() {
        const val USER_IDS_DELIMITER = ","
    }
}