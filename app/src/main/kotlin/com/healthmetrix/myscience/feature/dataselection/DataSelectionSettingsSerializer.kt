package com.healthmetrix.myscience.feature.dataselection

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import com.healthmetrix.myscience.features.dataselection.DataSelectionSettings
import java.io.InputStream
import java.io.OutputStream

object DataSelectionSettingsSerializer : Serializer<DataSelectionSettings> {
    override val defaultValue: DataSelectionSettings = DataSelectionSettings.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): DataSelectionSettings = try {
        @Suppress("BlockingMethodInNonBlockingContext")
        DataSelectionSettings.parseFrom(input)
    } catch (ex: InvalidProtocolBufferException) {
        throw CorruptionException("Cannot read proto.", ex)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun writeTo(t: DataSelectionSettings, output: OutputStream) = t.writeTo(output)
}
