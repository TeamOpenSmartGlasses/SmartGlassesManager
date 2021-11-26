package com.example.wearableaidisplaymoverio.sensors;

import java.nio.ByteBuffer;

public interface AudioChunkCallback{
    void onSuccess(ByteBuffer chunk);
}
