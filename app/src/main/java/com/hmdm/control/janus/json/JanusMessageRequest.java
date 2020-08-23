package com.hmdm.control.janus.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

public class JanusMessageRequest extends JanusPluginRequest {
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Body {
        private String request;
        private String id;

        public Body() {
        }

        public Body(String request, String id) {
            this.request = request;
            this.id = id;
        }

        public String getRequest() {
            return request;
        }

        public void setRequest(String request) {
            this.request = request;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    private Body body;

    public JanusMessageRequest() {
    }

    public JanusMessageRequest(String janus, String sessionId, String handleId) {
        super(janus, sessionId, handleId);
    }

    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }
}
