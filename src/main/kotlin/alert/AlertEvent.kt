package org.example.alert

import java.time.Instant

sealed interface AlertEvent {

    val timestamp: Instant

    data class SshLoginAlertEvent(
        val user: String,
        val ip: String,
        override val timestamp: Instant = Instant.now(),
    ) : AlertEvent

}