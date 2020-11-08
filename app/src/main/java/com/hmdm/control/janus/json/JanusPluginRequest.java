package com.hmdm.control.janus.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JanusPluginRequest extends JanusRequest {
    private String handle_id;


    public JanusPluginRequest() {
    }

    public JanusPluginRequest(String secret, String janus, String sessionId, String handleId) {
        super(secret, janus, true);
        setSession_id(sessionId);
        setHandle_id(handleId);
    }

    public String getHandle_id() {
        return handle_id;
    }

    public void setHandle_id(String handle_id) {
        this.handle_id = handle_id;
    }
}
