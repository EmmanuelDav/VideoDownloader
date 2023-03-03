package com.kunkunapp.allvideodowloader.webservice;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Result {
    @SerializedName("key")
    @Expose
    private String key;
    @SerializedName("mrk")
    @Expose
    private Integer mrk;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getMrk() {
        return mrk;
    }

    public void setMrk(Integer mrk) {
        this.mrk = mrk;
    }

}
