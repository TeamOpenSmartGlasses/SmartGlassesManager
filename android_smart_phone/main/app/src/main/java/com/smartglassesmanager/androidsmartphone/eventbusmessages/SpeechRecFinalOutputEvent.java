package com.smartglassesmanager.androidsmartphone.eventbusmessages;

import com.smartglassesmanager.androidsmartphone.DataStreamType;

import java.io.Serializable;

public class SpeechRecFinalOutputEvent implements Serializable {
    public String text;
    public long timestamp;
    public static final String eventId = "SpeechRecFinalOutputEvent";
    public static final DataStreamType dataStreamType = DataStreamType.TRANSCRIPTION_ENGLISH_STREAM;
    public SpeechRecFinalOutputEvent(String text, long timestamp){
        this.text = text;
        this.timestamp = timestamp;
    }
}
