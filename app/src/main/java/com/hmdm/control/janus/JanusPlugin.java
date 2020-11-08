package com.hmdm.control.janus;

import android.content.Context;

import com.hmdm.control.janus.json.JanusPollResponse;
import com.hmdm.control.janus.server.JanusServerApi;
import com.hmdm.control.janus.server.JanusServerApiFactory;

public abstract class JanusPlugin {
    protected JanusServerApi apiInstance;
    protected String secret;
    protected String sessionId;
    protected String handleId;
    protected String errorReason;

    protected JanusPollResponse pollingEvent;
    protected Object pollingEventLock = new Object();

    public String getHandleId() {
        return handleId;
    }

    public String getErrorReason() {
        return errorReason;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setHandleId(String handleId) {
        this.handleId = handleId;
    }

    public void init(Context context) {
        apiInstance = JanusServerApiFactory.getApiInstance(context);
        secret = JanusServerApiFactory.getSecret(context);
    }

    public void onWebRtcUp(final Context context) {
    }

    public void onPollingEvent(JanusPollResponse event) {
        synchronized (pollingEventLock) {
            pollingEvent = event;
            pollingEventLock.notify();
        }
    }

    public abstract String getName();
    public abstract int destroy();
}
