package com.healthmetrix.myscience.service.recontact

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object StateSerializer : KSerializer<RecontactService.Message.State> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("State", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: RecontactService.Message.State) =
        encoder.encodeString(value.name)

    override fun deserialize(decoder: Decoder) = try {
        RecontactService.Message.State.valueOf(decoder.decodeString())
    } catch (e: IllegalArgumentException) {
        RecontactService.Message.State.UNKNOWN
    }
}
