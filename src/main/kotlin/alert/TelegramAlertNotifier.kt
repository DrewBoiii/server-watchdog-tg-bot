package org.example.alert

import org.example.sender.MessageSender

class TelegramAlertNotifier(
    private val messageSender: MessageSender,
    private val chatId: Long,
) : AlertNotifier {

    override suspend fun notify(event: AlertEvent) {
        messageSender.sendMessage(chatId, format(event))
    }

    private fun format(event: AlertEvent): String = when (event) {
        is AlertEvent.SshLoginAlertEvent -> buildString {
            appendLine("🔐 Успешный SSH-вход")
            appendLine("👤 Пользователь: ${event.user}")
            appendLine("🌐 IP: ${event.ip}")
            append("🕒 ${event.timestamp}")
        }
    }
}