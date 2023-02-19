package com.wearableintelligencesystem.androidsmartphone.eventbusmessages;

import com.wearableintelligencesystem.androidsmartphone.sensors.AudioChunkCallback;

public class AudioChunkNewEvent {
    public byte [] thisChunk;

    public AudioChunkNewEvent(byte [] thisChunk){
        this.thisChunk = thisChunk;
    }
}
