package com.smartglassesmanager.androidsmartphone.eventbusmessages;

public class GoogleAudioChunkNewEvent {
    public byte [] thisChunk;

    public GoogleAudioChunkNewEvent(byte [] thisChunk){
        this.thisChunk = thisChunk;
    }
}
