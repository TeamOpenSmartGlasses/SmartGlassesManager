package com.teamopensmartglasses.sgmlib.events;

import org.json.JSONObject;

public class SendableIntentEvent {
    public String data;

    public SendableIntentEvent(String data)
    {
        this.data = data;
    }

    public SendableIntentEvent(JSONObject obj) {
        this.data = obj.toString();
    }
}
