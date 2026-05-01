package org.example.dto

data class CommandDto(
    val command: TextCommandEnum,
    val arguments: List<String>,
)