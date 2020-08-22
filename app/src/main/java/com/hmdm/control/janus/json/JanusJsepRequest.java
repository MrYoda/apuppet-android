package com.hmdm.control.janus.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JanusJsepRequest extends JanusPluginRequest {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Body {
        private String request;

        public Body() {
        }

        public Body(String request) {
            this.request = request;
        }

        public String getRequest() {
            return request;
        }

        public void setRequest(String request) {
            this.request = request;
        }
    }

    private Jsep jsep;
    private Body body;

    public Jsep getJsep() {
        return jsep;
    }

    public void setJsep(Jsep jsep) {
        this.jsep = jsep;
    }

    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }
}
