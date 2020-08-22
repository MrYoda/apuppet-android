package com.hmdm.control.janus.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Jsep implements Serializable {
    private String type;
    private String sdp;

    public Jsep() {
    }

    public Jsep(String type, String sdp) {
        this.type = type;
        this.sdp = sdp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSdp() {
        return sdp;
    }

    public void setSdp(String sdp) {
        this.sdp = sdp;
    }

    @Override
    public String toString() {
        return "{\"type\":\"" + type + "\",\"sdp\":\"" + sdp + "\"}";
    }
}
