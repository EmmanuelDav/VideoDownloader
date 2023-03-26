package com.cyberIyke.allvideodowloader.webservice


import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class SearchModel constructor() : Serializable {
    @SerializedName("gossip")
    @Expose
    var gossip: Gossip? = null
}