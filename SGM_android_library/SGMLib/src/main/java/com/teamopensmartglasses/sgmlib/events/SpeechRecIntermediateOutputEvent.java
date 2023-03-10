package com.teamopensmartglasses.sgmlib.events;

import java.io.Serializable;

public class SpeechRecIntermediateOutputEvent implements Serializable {
    public String text;
    public long timestamp;
    public static final String eventId = "SpeechRecIntermediateOutputEvent";
    public SpeechRecIntermediateOutputEvent(String text, long timestamp){
        this.text = text;
        this.timestamp = timestamp;
    }
}
