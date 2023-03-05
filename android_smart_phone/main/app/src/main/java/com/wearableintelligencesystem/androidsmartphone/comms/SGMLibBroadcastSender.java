package com.wearableintelligencesystem.androidsmartphone.comms;

import static com.teamopensmartglasses.sgmlib.SGMGlobalConstants.EVENT_BUNDLE;
import static com.teamopensmartglasses.sgmlib.SGMGlobalConstants.EVENT_ID;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.teamopensmartglasses.sgmlib.SGMGlobalConstants;

import java.io.Serializable;

public class SGMLibBroadcastSender {
    private String TAG = "WearableAi_SGMLibBroadcastSEnder";
    private String intentPkg;
    Context context;

    public SGMLibBroadcastSender (Context context) {
        this.context = context;
        this.intentPkg = SGMGlobalConstants.TO_TPA_FILTER;
    }

    public void sendEventToTPAs(String eventId, Serializable eventBundle) {
        Log.d(TAG, this.intentPkg);
        Log.d(TAG, "Sending event to TPAs");

        //setup intent to send
        Intent intent = new Intent();
        intent.setAction(intentPkg);
        intent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

        //load in and send data
        intent.putExtra(EVENT_ID, eventId);
        intent.putExtra(EVENT_BUNDLE, eventBundle);
        context.sendBroadcast(intent);
    }

}
