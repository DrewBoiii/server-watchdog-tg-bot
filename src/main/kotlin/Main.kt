package org.example

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    val botsApplication = TelegramBotsLongPollingApplication()
    try {
        botsApplication.registerBot(
            System.getenv("BOT_TOKEN") ?: throw IllegalStateException("BOT_TOKEN is not set"),
            ServerWatchdog()
        )
        println("Watchdog bot started...")
        Thread.currentThread().join()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}