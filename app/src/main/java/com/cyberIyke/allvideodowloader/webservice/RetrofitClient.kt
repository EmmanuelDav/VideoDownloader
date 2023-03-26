package com.cyberIyke.allvideodowloader.webservice

import com.cyberIyke.allvideodowloader.webservice.RetrofitClient
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tonyodev.fetch2.FetchConfiguration.Builder.build
import okhttp3.OkHttpClient
import okhttp3.OkHttpClient.Builder.build
import okhttp3.OkHttpClient.Builder.connectTimeout
import okhttp3.OkHttpClient.Builder.readTimeout
import okhttp3.OkHttpClient.Builder.writeTimeout
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitClient private constructor() {
    private val retrofit: Retrofit

    init {
        val httpClient: Builder = Builder()
        val interceptor: HttpLoggingInterceptor = HttpLoggingInterceptor()
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
        val client: OkHttpClient = httpClient
            .connectTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(5, TimeUnit.MINUTES)
            .readTimeout(5, TimeUnit.MINUTES)
            .build()
        val gson: Gson = GsonBuilder()
            .setLenient()
            .create()
        retrofit = Retrofit.Builder()
            .baseUrl(RetrofitClient.Companion.Search_Suggestion_Url)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(client)
            .build()
    }

    val api: RequestApi
        get() {
            return retrofit.create(RequestApi::class.java)
        }

    companion object {
        private val TAG: String = RetrofitClient::class.java.getSimpleName()
        private val mInstance: RetrofitClient? = null
        val Search_Suggestion_Url: String = "https://sugg.search.yahoo.net/"

        @get:Synchronized
        val instance: RetrofitClient
            get() {
                if (RetrofitClient.Companion.mInstance == null) {
                    RetrofitClient.Companion.mInstance = RetrofitClient()
                }
                return RetrofitClient.Companion.mInstance
            }
    }
}