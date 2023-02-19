package com.wearableintelligencesystem.androidsmartphone.sensors;

import java.nio.ByteBuffer;

public interface AudioChunkCallback{
    void onSuccess(ByteBuffer chunk);
}