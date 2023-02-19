package com.wearableintelligencesystem.androidsmartphone.eventbusmessages;

public class SpeechRecIntermediateOutputEvent {
    public String text;
    public long timestamp;

    public SpeechRecIntermediateOutputEvent(String text, long timestamp){
        this.text = text;
        this.timestamp = timestamp;
    }
}
