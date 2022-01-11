package com.google.mediapipe.apps.wearableai.sensors;

import android.graphics.Bitmap;


public interface CustomFrameAvailableListener {

    public void onFrame(Bitmap bitmap);
}
