package com.smartglassesmanager.androidsmartphone.eventbusmessages;

public class AudioChunkNewEvent {
    public byte [] thisChunk;

    public AudioChunkNewEvent(byte [] thisChunk){
        this.thisChunk = thisChunk;
    }
}
