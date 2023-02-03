package com.wearableintelligencesystem.androidsmartphone.comms;

import android.content.Context;
import android.content.Intent;

public class SGMBroadcastSender {
    Context context;

    public SGMBroadcastSender(Context context) {
        this.context = context;
    }

    public void broadcastData(String data) {
        Intent intent = new Intent();
        intent.putExtra("data", data);
        intent.setAction("com.teamsmartglasses.to3pa");
        intent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        context.sendBroadcast(intent);
    }
}
