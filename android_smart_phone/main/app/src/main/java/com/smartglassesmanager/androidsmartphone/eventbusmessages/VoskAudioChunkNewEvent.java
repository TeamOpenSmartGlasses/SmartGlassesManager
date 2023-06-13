package com.smartglassesmanager.androidsmartphone.eventbusmessages;

public class VoskAudioChunkNewEvent {
    public byte [] thisChunk;

    public VoskAudioChunkNewEvent(byte [] thisChunk){
        this.thisChunk = thisChunk;
    }
}
