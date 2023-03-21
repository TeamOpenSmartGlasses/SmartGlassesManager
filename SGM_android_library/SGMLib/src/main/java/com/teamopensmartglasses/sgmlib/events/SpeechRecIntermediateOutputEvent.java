package com.teamopensmartglasses.sgmlib.events;

import com.teamopensmartglasses.sgmlib.DataStreamType;

import java.io.Serializable;

public class SpeechRecIntermediateOutputEvent implements Serializable {
    public String text;
    public long timestamp;
    public static final String eventId = "SpeechRecIntermediateOutputEvent";
    public static final DataStreamType dataStreamType = DataStreamType.TRANSCRIPTION_ENGLISH_STREAM;
    public SpeechRecIntermediateOutputEvent(String text, long timestamp){
        this.text = text;
        this.timestamp = timestamp;
    }
}
