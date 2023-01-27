package com.healthmetrix.myscience.feature.login

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object LoginSettingsSerializer : Serializer<LoginSettings> {
    override val defaultValue: LoginSettings = LoginSettings.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): LoginSettings = try {
        @Suppress("BlockingMethodInNonBlockingContext")
        LoginSettings.parseFrom(input)
    } catch (ex: InvalidProtocolBufferException) {
        throw CorruptionException("Cannot read proto.", ex)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun writeTo(t: LoginSettings, output: OutputStream) = t.writeTo(output)
}
