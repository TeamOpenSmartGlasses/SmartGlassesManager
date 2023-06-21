package com.smartglassesmanager.androidsmartphone.hci;

import java.nio.ByteBuffer;

public interface AudioChunkCallback{
    void onSuccess(ByteBuffer chunk);
}