package com.cyberIyke.allvideodowloader.webservice

import com.cyberIyke.allvideodowloader.webservice.SearchModel
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface RequestApi {
    @GET("sg/?")
    fun getSearchResult(
        @Query("output") str: String?,
        @Query("nresults") i: Int,
        @Query("command") str2: String?
    ): Call<SearchModel?>?
}