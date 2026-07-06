package org.example.sender

interface MessageSender {

    suspend fun sendMessage(chatId: Long, text: String)

}