package org.example.handler

import org.telegram.telegrambots.meta.api.objects.message.Message

interface CommandMessageHandler {

    fun handle(message: Message): String

}