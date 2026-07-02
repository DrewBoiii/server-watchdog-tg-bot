package org.example.handler

import org.telegram.telegrambots.meta.api.objects.message.Message

interface CommandMessageHandler {

    suspend fun handle(message: Message): String

}