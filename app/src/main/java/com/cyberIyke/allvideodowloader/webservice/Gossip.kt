package com.cyberIyke.allvideodowloader.webservice


import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Gossip {
    @SerializedName("qry")
    @Expose
    var qry: String? = null

    @SerializedName("gprid")
    @Expose
    var gprid: String? = null

    @SerializedName("results")
    @Expose
    var results: List<Result>? = null
}