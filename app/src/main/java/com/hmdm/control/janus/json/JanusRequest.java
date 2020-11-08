package com.hmdm.control.janus.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.hmdm.control.Utils;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JanusRequest {
    private String janus;
    private String session_id;
    private String transaction;
    private String apisecret;

    public JanusRequest() {
    }

    public JanusRequest(String secret, String action, boolean generateTransactionId) {
        janus = action;
        apisecret = secret;
        if (generateTransactionId) {
            generateTransactionId();
        }
    }

    public void generateTransactionId() {
        transaction = Utils.generateTransactionId();
    }

    public String getJanus() {
        return janus;
    }

    public void setJanus(String janus) {
        this.janus = janus;
    }

    public String getSession_id() {
        return session_id;
    }

    public void setSession_id(String session_id) {
        this.session_id = session_id;
    }

    public String getTransaction() {
        return transaction;
    }

    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

    public String getApisecret() {
        return apisecret;
    }

    public void setApisecret(String apisecret) {
        this.apisecret = apisecret;
    }
}
