package com.hmdm.control.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.hmdm.control.Utils;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JanusSessionRequest {
    private String janus;
    private String session_id;
    private String transaction;
    private String plugin;

    public JanusSessionRequest() {
    }

    public JanusSessionRequest(String action, boolean generateTransactionId) {
        janus = action;
        if (generateTransactionId) {
            transaction = Utils.generateTransactionId();
        }
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

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }
}
