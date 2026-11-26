package org.example.alert

import kotlinx.coroutines.flow.Flow

fun interface AlertSource {
    fun events(): Flow<AlertEvent>
}