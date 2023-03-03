package com.wearableintelligencesystem.androidsmartphone.comms;

import static com.teamopensmartglasses.sgmlib.GlobalStrings.EVENT_BUNDLE;
import static com.teamopensmartglasses.sgmlib.GlobalStrings.EVENT_ID;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.teamopensmartglasses.sgmlib.events.CommandTriggeredEvent;
import com.teamopensmartglasses.sgmlib.events.ReferenceCardSimpleViewRequestEvent;
import com.teamopensmartglasses.sgmlib.events.RegisterCommandRequestEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.Serializable;

public class SGMLibBroadcastReceiver extends BroadcastReceiver {
    private String filterPkg;
    private Context context;
    public String TAG = "WearableAi_SGMLibBroadcastReceiver";

    public SGMLibBroadcastReceiver(Context context) {
        this.context = context;
        this.filterPkg = "com.teamopensmartglasses.from3pa";
        IntentFilter intentFilter = new IntentFilter(this.filterPkg);
        context.registerReceiver(this, intentFilter);
    }

    public void onReceive(Context context, Intent intent) {
        String eventId = intent.getStringExtra(EVENT_ID);
        Serializable serializedEvent = intent.getSerializableExtra(EVENT_BUNDLE);
        Log.d(TAG, "GOT EVENT ID: " + eventId);
        Log.i("Broadcastreceiver", "BroadcastReceiver Received");

        //map from id to event
        switch (eventId) {
            case ReferenceCardSimpleViewRequestEvent.eventId:
                Log.d(TAG, "Resending Reference Card event");
                EventBus.getDefault().post((ReferenceCardSimpleViewRequestEvent) serializedEvent);
                break;
            case RegisterCommandRequestEvent.eventId:
                Log.d(TAG, "Resending register command request event");
                EventBus.getDefault().post((RegisterCommandRequestEvent) serializedEvent);
                break;
        }
    }

    public void unregister(){
       context.unregisterReceiver(this);
    }
}

