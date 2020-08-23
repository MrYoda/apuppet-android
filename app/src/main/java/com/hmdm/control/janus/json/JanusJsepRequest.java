package com.hmdm.control.janus.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JanusJsepRequest extends JanusMessageRequest {

    public JanusJsepRequest() {
    }

    public JanusJsepRequest(String janus, String sessionId, String handleId) {
        super(janus, sessionId, handleId);
    }

    private Jsep jsep;

    public Jsep getJsep() {
        return jsep;
    }

    public void setJsep(Jsep jsep) {
        this.jsep = jsep;
    }
}
