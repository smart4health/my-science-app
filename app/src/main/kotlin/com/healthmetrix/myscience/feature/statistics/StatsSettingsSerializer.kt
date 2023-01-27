package com.healthmetrix.myscience.feature.statistics

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object StatsSettingsSerializer : Serializer<StatsSettings> {
    override val defaultValue: StatsSettings = StatsSettings.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): StatsSettings = try {
        @Suppress("BlockingMethodInNonBlockingContext")
        (StatsSettings.parseFrom(input))
    } catch (ex: InvalidProtocolBufferException) {
        throw CorruptionException("Cannot read proto.", ex)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun writeTo(t: StatsSettings, output: OutputStream) = t.writeTo(output)
}
