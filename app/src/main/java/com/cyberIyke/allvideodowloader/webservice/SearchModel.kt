package com.cyberIyke.allvideodowloader.webservice


import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class SearchModel : Serializable {
    @SerializedName("gossip")
    @Expose
    var gossip: Gossip? = null
}