package com.teamopensmartglasses.smartglassesmanager.eventbusmessages;

import android.graphics.Bitmap;

import java.io.Serializable;

public class SendBitmapViewRequestEvent implements Serializable {
    public Bitmap bmp;

    public SendBitmapViewRequestEvent(Bitmap newBbmp) {
        this.bmp = newBbmp;
    }
}
