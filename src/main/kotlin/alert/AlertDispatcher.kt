package org.example.alert

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import mu.KotlinLogging

class AlertDispatcher(
    private val sources: List<AlertSource>,
    private val scope: CoroutineScope,
) {
    private val logger = KotlinLogging.logger {}

    private val _events = MutableSharedFlow<AlertEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<AlertEvent> = _events.asSharedFlow()

    fun start() {
        sources.forEach { source ->
            scope.launch {
                try {
                    source.events().collect { event -> _events.emit(event) }
                } catch (e: Exception) {
                    logger.error(e) {
                        "Alert source ${source::class.simpleName} failed"
                    }
                }
            }
        }
    }
}