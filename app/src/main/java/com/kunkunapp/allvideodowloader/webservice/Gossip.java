package com.kunkunapp.allvideodowloader.webservice;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Gossip {

    @SerializedName("qry")
    @Expose
    private String qry;
    @SerializedName("gprid")
    @Expose
    private String gprid;
    @SerializedName("results")
    @Expose
    private List<Result> results = null;

    public String getQry() {
        return qry;
    }

    public void setQry(String qry) {
        this.qry = qry;
    }

    public String getGprid() {
        return gprid;
    }

    public void setGprid(String gprid) {
        this.gprid = gprid;
    }

    public List<Result> getResults() {
        return results;
    }

    public void setResults(List<Result> results) {
        this.results = results;
    }
}
