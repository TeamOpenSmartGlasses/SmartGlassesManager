package com.wearableintelligencesystem.androidsmartphone.comms;

import org.json.JSONObject;

public class SendableTranscriptEvent {
    public String data;

    public SendableTranscriptEvent(String data)
    {
        this.data = data;
    }

    public SendableTranscriptEvent(JSONObject obj) {
        this.data = obj.toString();
    }
}
