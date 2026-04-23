package org.example.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
@JsonIgnoreUnknownKeys
@OptIn(ExperimentalSerializationApi::class)
data class DockerContainerDto(
    @SerialName("Id")
    val id: String,
    @SerialName("Names")
    val names: List<String>,
    @SerialName("Image")
    val image: String,
    @SerialName("State")
    val state: String,
    @SerialName("Status")
    val status: String,
    @SerialName("Created")
    val created: Long,
)