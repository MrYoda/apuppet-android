package com.hmdm.control.janus;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;

import com.hmdm.control.Const;
import com.hmdm.control.SharingEngine;
import com.hmdm.control.janus.server.JanusServerApi;
import com.hmdm.control.janus.server.JanusServerApiFactory;

public class SharingEngineJanus extends SharingEngine {
    private JanusServerApi apiInstance;

    private JanusSession janusSession;
    private JanusTextRoomPlugin janusTextRoomPlugin;
    private JanusStreamingPlugin janusStreamingPlugin;

    private Handler handler = new Handler();

    @Override
    public void connect(final Context context, final String sessionId, final String password, final CompletionHandler completionHandler) {
        try {
            apiInstance = JanusServerApiFactory.getApiInstance(context);
        } catch (Exception e) {
            e.printStackTrace();
            completionHandler.onComplete(false, "Wrong server URL");
            return;
        }

        if (state != Const.STATE_DISCONNECTED) {
            completionHandler.onComplete(false, "Not disconnected");
            return;
        }

        reset();
        this.sessionId = sessionId;
        this.password = password;
        setState(Const.STATE_CONNECTING);

        janusSession = new JanusSession();
        janusSession.init(context);

        // Start Janus connection flow
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {
                int result = janusSession.create();
                if (result != Const.SUCCESS) {
                    errorReason = janusSession.getErrorReason();
                    return result;
                }

                janusSession.startPolling(context);

                janusTextRoomPlugin = new JanusTextRoomPlugin();
                janusTextRoomPlugin.init(context);
                result = janusSession.attachPlugin(janusTextRoomPlugin);
                if (result != Const.SUCCESS) {
                    return result;
                }

                // The successful flow is continued after creating a data channel
                janusTextRoomPlugin.createPeerConnection((success, errorReason) -> {
                    if (!success) {
                        handler.post(() -> completionHandler.onComplete(false, errorReason));
                    } else {
                        dataChannelCreated(context, completionHandler);
                    }
                }, eventListener);

                // Completion handler is needed here to handle errors
                janusTextRoomPlugin.setupRtcSession((success, errorReason) -> {
                    handler.post(() -> completionHandler.onComplete(false, errorReason));
                });

                return Const.SUCCESS;
            }

            @Override
            protected void onPostExecute(Integer result) {
                if (result != Const.SUCCESS) {
                    setState(Const.STATE_DISCONNECTED);
                    completionHandler.onComplete(false, errorReason);
                    reset();
                }
                // On success, the flow is continued in createPeerConnection when data channel is created
            }
        }.execute();
    }

    private void dataChannelCreated(Context context, CompletionHandler completionHandler) {
        new AsyncTask<Void, Void, Integer>() {

            @Override
            protected Integer doInBackground(Void... voids) {
                int result = janusTextRoomPlugin.createRoom(sessionId, password);
                if (result != Const.SUCCESS) {
                    return result;
                }

                result = janusTextRoomPlugin.joinRoom(username);
                if (result != Const.SUCCESS) {
                    return result;
                }

                // Streaming
                janusStreamingPlugin = new JanusStreamingPlugin();
                janusStreamingPlugin.init(context);
                result = janusSession.attachPlugin(janusStreamingPlugin);
                if (result != Const.SUCCESS) {
                    return result;
                }

                result = janusStreamingPlugin.create(sessionId, password, isAudio());

                return result;
            }

            @Override
            protected void onPostExecute(Integer result) {
                if (result != Const.SUCCESS) {
                    setState(Const.STATE_DISCONNECTED);
                    completionHandler.onComplete(false, errorReason);
                    reset();
                } else {
                    setState(Const.STATE_CONNECTED);
                    completionHandler.onComplete(true, null);
                }
            }
        }.execute();
    }

    @Override
    public void disconnect(final Context context, final CompletionHandler completionHandler) {
        errorReason = null;
        setState(Const.STATE_DISCONNECTING);

        janusSession.stopPolling(context);

        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {
                if (janusTextRoomPlugin != null) {
                    janusTextRoomPlugin.destroy();
                }
                if (janusStreamingPlugin != null) {
                    janusStreamingPlugin.destroy();
                }
                if (janusSession != null) {
                    janusSession.destroy();
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

    private void reset() {
        sessionId = null;
        password = null;
        janusSession = null;
        janusStreamingPlugin = null;
        janusTextRoomPlugin = null;
    }

    @Override
    public int getAudioPort() {
        if (janusStreamingPlugin != null) {
            return janusStreamingPlugin.getAudioPort();
        }
        return 0;
    }

    @Override
    public int getVideoPort() {
        if (janusStreamingPlugin != null) {
            return janusStreamingPlugin.getVideoPort();
        }
        return 0;
    }
}
