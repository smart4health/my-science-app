package com.healthmetrix.myscience.feature.sync

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object SyncSettingsSerializer : Serializer<SyncSettings> {
    override val defaultValue: SyncSettings = SyncSettings.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): SyncSettings = try {
        @Suppress("BlockingMethodInNonBlockingContext")
        SyncSettings.parseFrom(input)
    } catch (ex: InvalidProtocolBufferException) {
        throw CorruptionException("Cannot read proto.", ex)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun writeTo(t: SyncSettings, output: OutputStream) = t.writeTo(output)
}
