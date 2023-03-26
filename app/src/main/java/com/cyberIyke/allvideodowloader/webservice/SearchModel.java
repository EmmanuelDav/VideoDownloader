package com.cyberIyke.allvideodowloader.webservice;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class SearchModel implements Serializable {
    @SerializedName("gossip")
    @Expose
    private Gossip gossip;

    public Gossip getGossip() {
        return gossip;
    }

    public void setGossip(Gossip gossip) {
        this.gossip = gossip;
    }

}
