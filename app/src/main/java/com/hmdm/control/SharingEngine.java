package com.hmdm.control;

import android.content.Context;

public abstract class SharingEngine {
    protected int state = Const.STATE_DISCONNECTED;
    protected EventListener eventListener;
    protected StateListener stateListener;

    // Shareable session ID
    protected String sessionId;
    protected String password;
    protected String errorReason;
    protected String username = "Device";
    protected boolean audio;

    public int getState() {
        return state;
    }

    protected void setState(int state) {
        this.state = state;
        if (stateListener != null) {
            stateListener.onSharingApiStateChanged(state);
        }
    }

    public void setEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
    }

    public void setStateListener(StateListener stateListener) {
        this.stateListener = stateListener;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isAudio() {
        return audio;
    }

    public void setAudio(boolean audio) {
        this.audio = audio;
    }

    public abstract void connect(final Context context, final String sessionId, final String password, final CompletionHandler completionHandler);

    public abstract void disconnect(Context context, CompletionHandler completionHandler);

    public abstract int getAudioPort();

    public abstract int getVideoPort();

    public interface CompletionHandler {
        void onComplete(boolean success, String errorReason);
    }

    public interface EventListener {
        void onStartSharing(String username);
        void onStopSharing();
        void onRemoteControlEvent(String event);
    }

    public interface StateListener {
        void onSharingApiStateChanged(int state);
    }
}
