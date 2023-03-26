package com.cyberIyke.allvideodowloader.webservice;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RequestApi {
    @GET("sg/?")
    Call<SearchModel> getSearchResult(@Query("output") String str, @Query("nresults") int i, @Query("command") String str2);
}
