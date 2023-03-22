package com.smartglassesmanager.androidsmartphone.comms;

import org.json.JSONObject;

public interface VolleyCallback {
    void onSuccess(JSONObject result);
    void onFailure();
}
