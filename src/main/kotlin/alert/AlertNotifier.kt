package org.example.alert

interface AlertNotifier {
    suspend fun notify(event: AlertEvent)
}