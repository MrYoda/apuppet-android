package com.hmdm.control.janus.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JanusResponse {
    private String janus;
    private String transaction;

    public class Data {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "{\"id\":\"" + id + "\"}";
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public class Error {
        private Integer code;
        private String reason;

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
        @Override
        public String toString() {
            return "{\"code\":\"" + code + "\",\"reason\":\"" + reason + "\"}";
        }
    }

    private Data data;
    private Error error;

    public String getJanus() {
        return janus;
    }

    public void setJanus(String janus) {
        this.janus = janus;
    }

    public String getTransaction() {
        return transaction;
    }

    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "{\"janus\":\"" + janus + "\",\"transaction\":\"" + transaction + "\",\"data\":" + (data != null ? data.toString() : null) +
                ",\"error\":" + (error != null ? error.toString() : null) + "}";
    }
}
