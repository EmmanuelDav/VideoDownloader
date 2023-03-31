package com.cyberIyke.allvideodowloader.webservice

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient

import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitClient {
    private val retrofit: Retrofit

    init {
        val httpClient: OkHttpClient.Builder = OkHttpClient.Builder()
        val interceptor: HttpLoggingInterceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
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
        private val TAG: String = RetrofitClient::class.java.simpleName
        private var mInstance: RetrofitClient? = null
        const val Search_Suggestion_Url: String = "https://sugg.search.yahoo.net/"

        @get:Synchronized
        val instance: RetrofitClient
            get() {
                if (mInstance == null) {
                    mInstance = RetrofitClient()
                }
                return mInstance as RetrofitClient
            }
    }
}