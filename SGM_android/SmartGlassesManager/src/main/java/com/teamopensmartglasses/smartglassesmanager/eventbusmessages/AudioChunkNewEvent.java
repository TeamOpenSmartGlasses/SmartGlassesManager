package com.teamopensmartglasses.smartglassesmanager.eventbusmessages;

public class AudioChunkNewEvent {
    public byte [] thisChunk;

    public AudioChunkNewEvent(byte [] thisChunk){
        this.thisChunk = thisChunk;
    }
}
