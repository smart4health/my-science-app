package com.healthmetrix.myscience.di

import ca.uhn.fhir.context.FhirContext
import com.healthmetrix.myscience.DataProvConfig
import com.healthmetrix.myscience.service.deident.BundleConverterFactory
import com.healthmetrix.myscience.service.deident.DeidentService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Credentials
import okhttp3.OkHttpClient
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DeidentServiceModule {
    @Provides
    @Singleton
    fun provideBundleConverterFactory(fhirContext: FhirContext): BundleConverterFactory =
        BundleConverterFactory(fhirContext)

    @Provides
    @Singleton
    @Named("deidentOkHttpClient")
    fun provideDeidentOkHttpClient(
        dataProvConfig: DataProvConfig,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain
                .request()
                .newBuilder()
                .addHeader(
                    "Authorization",
                    Credentials.basic(
                        dataProvConfig.deident.user,
                        dataProvConfig.deident.pass,
                    ),
                )
                .build()
                .let(chain::proceed)
        }.build()

    @Provides
    @Singleton
    fun provideDeidentService(
        dataProvConfig: DataProvConfig,
        bundleConverterFactory: BundleConverterFactory,
        @Named("deidentOkHttpClient")
        okHttpClient: OkHttpClient,
    ): DeidentService = DeidentService(
        baseUrl = dataProvConfig.deident.baseUrl,
        bundleConverterFactory = bundleConverterFactory,
        client = okHttpClient,
        json = Json { ignoreUnknownKeys = true },
    )
}
