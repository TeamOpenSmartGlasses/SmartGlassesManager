package com.smartglassesmanager.androidsmartphone.sensors;

import java.nio.ByteBuffer;

public interface AudioChunkCallback{
    void onSuccess(ByteBuffer chunk);
}