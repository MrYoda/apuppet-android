package com.hmdm.control;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.hmdm.control.json.JanusSessionRequest;
import com.hmdm.control.json.JanusSessionResponse;
import com.hmdm.control.server.JanusServerApi;
import com.hmdm.control.server.ServerApiFactory;
import com.hmdm.control.server.ServerApiHelper;

import retrofit2.Response;

public class SharingApi {
    private static SharingApi instance;

    private JanusServerApi apiInstance;

    public interface CompletionHandler {
        void onComplete(boolean success, String errorReason);
    }

    public interface EventListener {
        void onRemoteControlEvent(String event);
    }

    public interface StateListener {
        void onSharingApiStateChanged(int state);
    }

    public static SharingApi getInstance() {
        if (instance == null) {
            instance = new SharingApi();
        }
        return instance;
    }

    private int state = Const.STATE_DISCONNECTED;

    private EventListener eventListener;
    private StateListener stateListener;

    // Shareable session ID = text room ID
    private String sessionId;
    private String password;
    private String username = "Device";

    // Set after attaching to streaming plugin
    private int audioPort;
    private int videoPort;

    private String errorReason;
    private String janusSessionId;

    public void connect(final Context context, final String sessionId, final String password, final CompletionHandler completionHandler) {
        apiInstance = ServerApiFactory.getApiInstance(context);

        if (state != Const.STATE_DISCONNECTED) {
            completionHandler.onComplete(false, "Not disconnected");
            return;
        }

        janusSessionId = null;
        errorReason = null;
        this.sessionId = sessionId;
        this.password = password;
        setState(Const.STATE_CONNECTING);

        // Create Janus session
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {
                Response<JanusSessionResponse> response = ServerApiHelper.execute(apiInstance.session(new JanusSessionRequest("create", true)), "create session");
                if (response == null) {
                    errorReason = "Network error";
                    return Const.NETWORK_ERROR;
                }
                if (response.body() != null && response.body().getJanus().equalsIgnoreCase("success") && response.body().getData() != null) {
                    janusSessionId = response.body().getData().getId();
                } else {
                    errorReason = "Server error";
                    Log.w(Const.LOG_TAG, "Wrong server response: " + response.body().toString());
                    return Const.SERVER_ERROR;
                }
                return Const.SUCCESS;
            }

            @Override
            protected void onPostExecute(Integer result) {
                if (result != Const.SUCCESS) {
                    setState(Const.STATE_DISCONNECTED);
                    completionHandler.onComplete(false, errorReason);
                    reset();
                } else {
                    // Attach to Janus text room to get control messages from the peer
                    attachTextRoom(context, completionHandler);
                }
            }
        }.execute();
    }

    // Attach Janus text room plugin
    private void attachTextRoom(final Context context, final CompletionHandler completionHandler) {
        // TODO
    }

    // Create a new text room
    private void createTextRoom(final Context context, final CompletionHandler completionHandler) {
        // TODO
    }

    // Join the created text room
    private void joinTextRoom(final Context context, final CompletionHandler completionHandler) {
        // TODO
    }

    // Attach Janus streaming plugin
    private void attachStreaming(final Context context, final CompletionHandler completionHandler) {
        // TODO
    }

    // Create new streaming to get audio and video ports
    private void createStreaming(final Context context, final CompletionHandler completionHandler) {
        // TODO
    }

    public void disconnect(final Context context, final CompletionHandler completionHandler) {
        errorReason = null;
        setState(Const.STATE_DISCONNECTING);

        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {
                Response<JanusSessionResponse> response = ServerApiHelper.execute(apiInstance.session(new JanusSessionRequest("destroy", true)), "create session");
                janusSessionId = null;
                if (response == null) {
                    errorReason = "Network error";
                    return Const.NETWORK_ERROR;
                }
                return Const.SUCCESS;
            }

            @Override
            protected void onPostExecute(Integer result) {
                // Not really fair, but it's unclear how to handle destroying errors!
                setState(Const.STATE_DISCONNECTED);
                completionHandler.onComplete(result == Const.SUCCESS, errorReason);
                reset();
            }
        }.execute();

    }

    public void reset() {
        audioPort = 0;
        videoPort = 0;
        sessionId = null;
        password = null;
        janusSessionId = null;
    }

    public int getState() {
        return state;
    }

    public int getAudioPort() {
        return audioPort;
    }

    public int getVideoPort() {
        return videoPort;
    }

    public void setEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
    }

    public void setStateListener(StateListener stateListener) {
        this.stateListener = stateListener;
    }

    private void setState(int state) {
        this.state = state;
        if (stateListener != null) {
            stateListener.onSharingApiStateChanged(state);
        }
    }
}
