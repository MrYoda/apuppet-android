package com.hmdm.control.janus;

import android.content.Context;
import android.util.Log;

import com.hmdm.control.Const;
import com.hmdm.control.ServerApiHelper;
import com.hmdm.control.SharingEngine;
import com.hmdm.control.Utils;
import com.hmdm.control.janus.json.JanusJsepRequest;
import com.hmdm.control.janus.json.JanusResponse;
import com.hmdm.control.janus.json.Jsep;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import retrofit2.Response;

public class JanusTextRoomPlugin extends JanusPlugin {

    private String roomId;
    private String password;
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private DataChannel dataChannel;
    private boolean joined;

    private boolean dcResult;
    private Object dcResultLock = new Object();

    @Override
    public String getName() {
        return Const.JANUS_PLUGIN_TEXTROOM;
    }

    public String getRoomId() {
        return roomId;
    }

    @Override
    public void init(Context context) {
        super.init(context);
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(context)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);
        peerConnectionFactory = PeerConnectionFactory.builder().createPeerConnectionFactory();
    }

    public void createPeerConnection(final SharingEngine.CompletionHandler completionHandler, final SharingEngine.EventListener eventListener) {
        // No ICE servers required for textroom: textroom messages are all coming through Janus
        List<PeerConnection.IceServer> iceServers = new LinkedList<>();
        peerConnection = peerConnectionFactory.createPeerConnection(iceServers, new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                Log.d(Const.LOG_TAG, "Textroom plugin: signalingState changed to " + signalingState);
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.d(Const.LOG_TAG, "Textroom plugin: iceConnectionState changed to " + iceConnectionState);
                // ICE connection is not implemented properly, but it doesn't matter
                // because the traffic is going through Janus which is not behind the firewall
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {
                Log.d(Const.LOG_TAG, "Textroom plugin: iceConnectionReceivingChange: " + b);
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                Log.d(Const.LOG_TAG, "Textroom plugin: iceGatheringState changed to " + iceGatheringState);
            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d(Const.LOG_TAG, "Textroom plugin: iceCandidate: " + iceCandidate.toString());
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
                Log.d(Const.LOG_TAG, "Textroom plugin: iceCandidateRemoved");
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.d(Const.LOG_TAG, "Textroom plugin: onAddStream: " + mediaStream.toString());
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                Log.d(Const.LOG_TAG, "Textroom plugin: onRemoveStream: " + mediaStream.toString());
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                Log.d(Const.LOG_TAG, "Textroom plugin: onDataChannel, id=" + dataChannel.id() + ", label=" + dataChannel.label());
                dataChannel.registerObserver(new DataChannel.Observer() {
                    @Override
                    public void onBufferedAmountChange(long l) {
                        Log.d(Const.LOG_TAG, "Textroom plugin: dataChannel - onBufferedAmountChange=" + l);
                    }

                    @Override
                    public void onStateChange() {
                        Log.d(Const.LOG_TAG, "Textroom plugin: dataChannel - onStateChange");
                    }

                    @Override
                    public void onMessage(DataChannel.Buffer buffer) {
                        String message = Utils.byteBufferToString(buffer.data);
                        Log.d(Const.LOG_TAG, "Textroom plugin: got message from DataChannel: " + message);

                        try {
                            JSONObject jsonObject = new JSONObject(message);
                            String type = jsonObject.optString("textroom");
                            if ("join".equalsIgnoreCase(type)) {
                                if (!checkJoined()) {
                                    return;
                                }
                                Log.d(Const.LOG_TAG, "Remote control agent connected, starting sharing");
                                String username = jsonObject.optString("username");
                                if (eventListener != null) {
                                    eventListener.onStartSharing(username);
                                }
                            } else if ("message".equalsIgnoreCase(type)) {
                                if (!checkJoined()) {
                                    return;
                                }
                                String text = jsonObject.optString("text");
                                if (eventListener != null) {
                                    Log.d(Const.LOG_TAG, "Dispatching message: " + text);
                                    eventListener.onRemoteControlEvent(text);
                                }
                            } else if ("leave".equalsIgnoreCase(type)) {
                                if (!checkJoined()) {
                                    return;
                                }
                                Log.d(Const.LOG_TAG, "Remote control agent disconnected, stopping sharing");
                                if (eventListener != null) {
                                    eventListener.onStopSharing();
                                }
                            } else if ("success".equalsIgnoreCase(type)) {
                                JSONArray list = jsonObject.optJSONArray("list");
                                if (list != null) {
                                    // This is the response to test request, nothing to do
                                    return;
                                }
                                synchronized (dcResultLock) {
                                    dcResult = true;
                                    dcResultLock.notify();
                                }
                            } else if ("error".equalsIgnoreCase(type)) {
                                synchronized (dcResultLock) {
                                    dcResult = false;
                                    dcResultLock.notify();
                                }
                            } else {
                                Log.d(Const.LOG_TAG, "Ignoring this message");
                            }

                        } catch (JSONException e) {
                            Log.w(Const.LOG_TAG, "Failed to parse JSON, ignoring!");
                        }
                    }
                });
                // Here's a final point of data channel creation
                completionHandler.onComplete(true, null);
            }

            @Override
            public void onRenegotiationNeeded() {
                Log.d(Const.LOG_TAG, "Textroom plugin: onRenegotiationNeeded");
            }

            @Override
            public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                Log.d(Const.LOG_TAG, "Textroom plugin: onAddTrack");
            }
        });

    }

    private boolean checkJoined() {
        if (!joined) {
            Log.w(Const.LOG_TAG, "Ignoring message because we're not yet joined the textroom");
            return false;
        }
        return true;
    }


    private JanusJsepRequest createJsepRequest(String requestType) {
        JanusJsepRequest request = new JanusJsepRequest();
        request.setJanus("message");
        request.setSession_id(getSessionId());
        request.setHandle_id(getHandleId());
        request.generateTransactionId();
        request.setBody(new JanusJsepRequest.Body(requestType));
        return request;
    }

    public void setupRtcSession(final SharingEngine.CompletionHandler completionHandler) {
        errorReason = null;
        JanusJsepRequest offerRequest = createJsepRequest("setup");
        Response<JanusResponse> response = ServerApiHelper.execute(apiInstance.sendJsep(getSessionId(), getHandleId(), offerRequest), "get JSEP offer");
        if (response == null) {
            errorReason = "Network error";
            completionHandler.onComplete(false, errorReason);
            return;
        }
        if (response.body() != null && response.body().getJanus().equalsIgnoreCase("ack")) {
            Log.i(Const.LOG_TAG, "Got response to JSEP offer, waiting for event");

            synchronized (pollingEventLock) {
                try {
                    pollingEventLock.wait();
                } catch (InterruptedException e) {
                    errorReason = "Interrupted";
                    completionHandler.onComplete(false, errorReason);
                    return;
                }
            }
            Jsep jsepData = pollingEvent.getJsep();
            if (jsepData == null) {
                errorReason = "Server error";
                Log.w(Const.LOG_TAG, "Missing JSEP: " + response.body().toString());
                completionHandler.onComplete(false, errorReason);
                return;
            }
            peerConnection.setRemoteDescription(new SdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                    Log.i(Const.LOG_TAG, "RemoteDescription - create success");
                }

                @Override
                public void onSetSuccess() {
                    Log.i(Const.LOG_TAG, "RemoteDescription - success");
                    // Proceed with SDP asynchronously
                    createSessionAnswer(completionHandler);
                }

                @Override
                public void onCreateFailure(String s) {
                    Log.i(Const.LOG_TAG, "RemoteDescription - create failure: " + s);
                }

                @Override
                public void onSetFailure(String s) {
                    errorReason = "RemoteDescription - failure: " + s;
                    Log.w(Const.LOG_TAG, errorReason);
                    completionHandler.onComplete(false, errorReason);
                }
            }, new SessionDescription(SessionDescription.Type.OFFER, jsepData.getSdp()));
        }
    }

    private void createSessionAnswer(final SharingEngine.CompletionHandler completionHandler) {
        MediaConstraints constraints = new MediaConstraints();
        peerConnection.createAnswer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.i(Const.LOG_TAG, "createAnswer - create success");
                // Proceed with setting local session description asynchronously
                setLocalSessionDescription(sessionDescription, completionHandler);
            }

            @Override
            public void onSetSuccess() {
                Log.i(Const.LOG_TAG, "createAnswer - success");
            }

            @Override
            public void onCreateFailure(String s) {
                errorReason = "createAnswer - create failure: " + s;
                Log.w(Const.LOG_TAG, errorReason);
                completionHandler.onComplete(false, errorReason);
            }

            @Override
            public void onSetFailure(String s) {
                Log.i(Const.LOG_TAG, "createAnswer - failure: " + s);
            }
        }, constraints);
    }

    private void setLocalSessionDescription(final SessionDescription sessionDescription, final SharingEngine.CompletionHandler completionHandler) {
        peerConnection.setLocalDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.i(Const.LOG_TAG, "LocalDescription - create success");
            }

            @Override
            public void onSetSuccess() {
                Log.i(Const.LOG_TAG, "LocalDescription - success");
                // Proceed with SDP asynchronously
                sendSessionDescriptionAck(completionHandler);
            }

            @Override
            public void onCreateFailure(String s) {
                Log.i(Const.LOG_TAG, "LocalDescription - create failure: " + s);
            }

            @Override
            public void onSetFailure(String s) {
                errorReason = "LocalDescription - failure: " + s;
                Log.w(Const.LOG_TAG, errorReason);
                completionHandler.onComplete(false, errorReason);
            }
        }, sessionDescription);
    }

    private void sendSessionDescriptionAck(final SharingEngine.CompletionHandler completionHandler) {
        JanusJsepRequest offerRequest = createJsepRequest("ack");
        offerRequest.setJsep(new Jsep("answer", peerConnection.getLocalDescription().description));
        Response<JanusResponse> response = ServerApiHelper.execute(apiInstance.sendJsep(getSessionId(), getHandleId(), offerRequest), "send JSEP ack");
        if (response == null) {
            errorReason = "Network error";
            completionHandler.onComplete(false, errorReason);
            return;
        }
        if (response.body() != null && response.body().getJanus().equalsIgnoreCase("ack")) {
            Log.i(Const.LOG_TAG, "Got response to JSEP offer, waiting for event");

            synchronized (pollingEventLock) {
                try {
                    pollingEventLock.wait();
                } catch (InterruptedException e) {
                    errorReason = "Interrupted";
                    completionHandler.onComplete(false, errorReason);
                    return;
                }
            }
            if (pollingEvent.getPlugindata() == null || pollingEvent.getPlugindata().getData() == null ||
                    !"ok".equalsIgnoreCase(pollingEvent.getPlugindata().getData().getResult())) {
                // Failure
                errorReason = "Server error";
                Log.w(Const.LOG_TAG, "Wrong server response: " + pollingEvent.toString());
                completionHandler.onComplete(false, errorReason);
                return;
            }
            // Success
        }
    }

    @Override
    public void onWebRtcUp() {
        Log.i(Const.LOG_TAG, "WebRTC is up!");
        DataChannel.Init init = new DataChannel.Init();
        dataChannel = peerConnection.createDataChannel("Trick", init);

        // We need to send something into the data channel, otherwise it won't be initialized
        String message = "{\"textroom\":\"list\",\"transaction\":\"" + Utils.generateTransactionId() + "\"}";
        sendToDataChannel(message);

    }

    private void sendToDataChannel(String message) {
        Log.d(Const.LOG_TAG, "Sending message: " + message);
        ByteBuffer data = Utils.stringToByteBuffer(message);
        DataChannel.Buffer buffer = new DataChannel.Buffer(data, false);
        dataChannel.send(buffer);
    }

    public int createRoom(String roomId, String password) {
        this.roomId = roomId;
        this.password = password;

        String createMessage = "{\"textroom\":\"create\",\"is_private\":false,\"permanent\":false,\"transaction\":\"" + Utils.generateTransactionId() +
                "\",\"room\":\"" + roomId + "\",\"pin\":\"" + password + "\"}";

        sendToDataChannel(createMessage);
        synchronized (dcResultLock) {
            try {
                dcResultLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                errorReason = "Interrupted";
                return Const.INTERNAL_ERROR;
            }
            if (!dcResult) {
                errorReason = "Failed to create a text room";
                return Const.SERVER_ERROR;
            }
        }
        return Const.SUCCESS;
    }

    public int joinRoom(String username) {
        String joinMessage = "{\"textroom\":\"join\",\"room\":\"" + roomId + "\",\"username\":\"" + username + "\",\"display\":\"" + username +
                "\",\"pin\":\"" + password + "\", \"transaction\":\"" + Utils.generateTransactionId() + "\"}";

        sendToDataChannel(joinMessage);
        synchronized (dcResultLock) {
            try {
                dcResultLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                errorReason = "Interrupted";
                return Const.INTERNAL_ERROR;
            }
            if (!dcResult) {
                errorReason = "Failed to join a text room";
                return Const.SERVER_ERROR;
            }
        }
        joined = true;
        return Const.SUCCESS;
    }

    @Override
    public int destroy() {
        if (dataChannel != null) {
            String destroyMessage = "{\"textroom\":\"destroy\",\"room\":\"" + roomId + "\"permanent\":false,\"transaction\":\"" + Utils.generateTransactionId() + "\"}";
            ByteBuffer data = Utils.stringToByteBuffer(destroyMessage);
            DataChannel.Buffer buffer = new DataChannel.Buffer(data, false);
            if (!dataChannel.send(buffer)) {
                errorReason = "Network error";
                return Const.NETWORK_ERROR;
            }
        }
        return Const.SUCCESS;
    }
}
