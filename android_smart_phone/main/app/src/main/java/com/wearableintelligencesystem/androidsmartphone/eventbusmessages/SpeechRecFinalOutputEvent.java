package com.wearableintelligencesystem.androidsmartphone.eventbusmessages;

public class SpeechRecFinalOutputEvent {
    public String text;
    public long timestamp;

    public SpeechRecFinalOutputEvent(String text, long timestamp){
        this.text = text;
        this.timestamp = timestamp;
    }
}
