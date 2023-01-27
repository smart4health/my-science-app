package com.healthmetrix.myscience.feature.messages

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object MessageSettingsSerializer : Serializer<MessagesSettings> {
    override val defaultValue: MessagesSettings = MessagesSettings.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): MessagesSettings = try {
        @Suppress("BlockingMethodInNonBlockingContext")
        MessagesSettings.parseFrom(input)
    } catch (ex: InvalidProtocolBufferException) {
        throw CorruptionException("Cannot read proto.", ex)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun writeTo(t: MessagesSettings, output: OutputStream) = t.writeTo(output)
}
