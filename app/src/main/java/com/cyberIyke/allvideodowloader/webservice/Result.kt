package com.cyberIyke.allvideodowloader.webservice

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Result {
    @SerializedName("key")
    @Expose
    var key: String? = null

    @SerializedName("mrk")
    @Expose
    var mrk: Int? = null
}