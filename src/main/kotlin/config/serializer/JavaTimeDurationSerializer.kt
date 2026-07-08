package org.example.config.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Duration

class JavaTimeDurationSerializer : KSerializer<Duration> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("java.time.Duration")

    override fun serialize(encoder: Encoder, value: Duration) =
        encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): Duration =
        Duration.parse(decoder.decodeString())
}