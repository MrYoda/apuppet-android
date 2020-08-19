package com.hmdm.control.server;

import com.hmdm.control.json.JanusSessionRequest;
import com.hmdm.control.json.JanusSessionResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface JanusServerApi {

    @POST("/janus")
    @Headers("Content-Type: application/json")
    Call<JanusSessionResponse> session(@Body JanusSessionRequest request);

}
