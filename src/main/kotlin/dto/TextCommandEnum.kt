package org.example.dto

enum class TextCommandEnum(
    val command: String,
) {

    UNKNOWN("Unknown command"),
    START("/start"),
    STATUS("/status"),
    UPTIME("/uptime"),
    SSH("/ssh"),
    SSH_FAILED("/ssh_failed"),
    DOCKER_ACTIVE_SERVICES("/docker_active_services"),
    ;
}