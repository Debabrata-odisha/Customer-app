package com.example.data.supabase

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class SupabaseClientProvider(val config: SupabaseConfig) {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private var currentUrl: String = ""
    private var currentToken: String? = null
    private var cachedApi: SupabaseApi? = null

    val realtimeClient: SupabaseRealtimeClient by lazy {
        SupabaseRealtimeClient(config)
    }

    @Synchronized
    fun getApi(): SupabaseApi {
        val configuredUrl = if (config.projectUrl.isNotEmpty()) {
            if (config.projectUrl.endsWith("/")) config.projectUrl else "${config.projectUrl}/"
        } else {
            "https://placeholder.supabase.co/"
        }

        val token = config.accessToken

        if (cachedApi != null && currentUrl == configuredUrl && currentToken == token) {
            return cachedApi!!
        }

        currentUrl = configuredUrl
        currentToken = token

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()

            val anonKey = config.anonKey
            if (anonKey.isNotEmpty()) {
                builder.header("apikey", anonKey)
            }

            val bearerToken = config.accessToken ?: anonKey
            if (!bearerToken.isNullOrEmpty()) {
                builder.header("Authorization", "Bearer $bearerToken")
            }

            builder.header("Accept", "application/json")
            builder.header("Content-Type", "application/json")

            chain.proceed(builder.build())
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(configuredUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        val api = retrofit.create(SupabaseApi::class.java)
        cachedApi = api
        return api
    }

    fun resetClient() {
        cachedApi = null
        realtimeClient.disconnect()
        if (config.isConfigured) {
            realtimeClient.connect()
        }
    }
}
