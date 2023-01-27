package com.healthmetrix.myscience.feature.sync.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WriteToFileUseCase @Inject constructor(
    @ApplicationContext
    private val applicationContext: Context,
) {
    suspend operator fun invoke(filename: String, contents: String) = withContext(Dispatchers.IO) {
        @Suppress("BlockingMethodInNonBlockingContext")
        applicationContext.openFileOutput(filename, Context.MODE_PRIVATE).use {
            it.write(contents.toByteArray(Charsets.UTF_8))
        }
    }
}
