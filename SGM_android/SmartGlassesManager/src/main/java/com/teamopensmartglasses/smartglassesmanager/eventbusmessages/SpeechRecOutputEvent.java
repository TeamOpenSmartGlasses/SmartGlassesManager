package com.teamopensmartglasses.smartglassesmanager.eventbusmessages;

import java.io.Serializable;

public class SpeechRecOutputEvent implements Serializable {
    public String text;
    public long timestamp;
    public boolean isFinal;
    public static final String eventId = "SpeechRecOutputEvent";

    public SpeechRecOutputEvent(String text, long timestamp, boolean isFinal){
        this.text = text;
        this.timestamp = timestamp;
        this.isFinal = isFinal;
    }
}
