package com.teamopensmartglasses.sgmlib;

import org.json.JSONObject;

public class ReceivedIntentEvent {
    public String data;

    public ReceivedIntentEvent(String data)
    {
        this.data = data;
    }

    public ReceivedIntentEvent(JSONObject obj) {
        this.data = obj.toString();
    }
}
