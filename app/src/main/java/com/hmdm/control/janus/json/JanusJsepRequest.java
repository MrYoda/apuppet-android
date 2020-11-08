package com.hmdm.control.janus.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JanusJsepRequest extends JanusMessageRequest {

    public JanusJsepRequest() {
    }

    public JanusJsepRequest(String secret, String janus, String sessionId, String handleId) {
        super(secret, janus, sessionId, handleId);
    }

    private Jsep jsep;

    public Jsep getJsep() {
        return jsep;
    }

    public void setJsep(Jsep jsep) {
        this.jsep = jsep;
    }
}
