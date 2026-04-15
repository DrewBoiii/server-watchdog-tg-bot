package org.example.sender

interface MessageSender {

    fun sendMessage(chatId: Long, text: String)

}