package com.hmdm.control;

import android.util.Log;

import retrofit2.Call;
import retrofit2.Response;

public class ServerApiHelper {
    public static Response execute(Call call, String description) {
        Response response = null;

        try {
            response = call.execute();
        } catch (Exception e) {
            Log.w(Const.LOG_TAG, "Failed to " + description + ": " + e.getMessage());
            return null;
        }
        if (response == null) {
            Log.w(Const.LOG_TAG, "Failed to " + description + ": network error");
            return null;
        }

        if (!response.isSuccessful()) {
            Log.w(Const.LOG_TAG, "Failed to " + description + ": bad server response: " + response.message());
            return null;
        }

        return response;
    }
}
