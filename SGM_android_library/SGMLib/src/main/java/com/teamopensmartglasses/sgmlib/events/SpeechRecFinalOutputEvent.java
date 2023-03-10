package com.teamopensmartglasses.sgmlib.events;

import java.io.Serializable;

public class SpeechRecFinalOutputEvent implements Serializable {
    public String text;
    public long timestamp;
    public static final String eventId = "SpeechRecFinalOutputEvent";
    public SpeechRecFinalOutputEvent(String text, long timestamp){
        this.text = text;
        this.timestamp = timestamp;
    }
}
