package com.hmdm.control.janus.server;

import com.hmdm.control.janus.json.JanusAttachRequest;
import com.hmdm.control.janus.json.JanusPollResponse;
import com.hmdm.control.janus.json.JanusRequest;
import com.hmdm.control.janus.json.JanusResponse;
import com.hmdm.control.janus.json.JanusJsepRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface JanusServerApi {

    @POST("/janus")
    @Headers("Content-Type: application/json")
    Call<JanusResponse> createSession(@Body JanusRequest request);

    @POST("/janus/{session}")
    @Headers("Content-Type: application/json")
    Call<JanusResponse> attachPlugin(@Path("session") String sessionId, @Body JanusAttachRequest request);

    @POST("/janus/{session}/{handle}")
    @Headers("Content-Type: application/json")
    Call<JanusResponse> sendJsep(@Path("session") String sessionId, @Path("handle") String handleId, @Body JanusJsepRequest request);

    @POST("/janus/{session}")
    @Headers("Content-Type: application/json")
    Call<JanusResponse> destroySession(@Path("session") String sessionId, @Body JanusRequest request);

    @GET("/janus/{session}")
    Call<JanusPollResponse> poll(@Path("session") String sessionId);

}
