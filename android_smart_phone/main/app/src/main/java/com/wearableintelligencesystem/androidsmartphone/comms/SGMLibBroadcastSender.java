package com.wearableintelligencesystem.androidsmartphone.comms;

import static com.teamopensmartglasses.sgmlib.GlobalStrings.EVENT_BUNDLE;
import static com.teamopensmartglasses.sgmlib.GlobalStrings.EVENT_ID;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.teamopensmartglasses.sgmlib.events.CommandTriggeredEvent;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.Serializable;

public class SGMLibBroadcastSender {
    private String TAG = "WearableAi_SGMLibBroadcastSEnder";
    private String intentPkg;
    Context context;

    public SGMLibBroadcastSender (Context context) {
        this.context = context;
        this.intentPkg = "com.teamopensmartglasses.to3pa";

        //register event bus subscribers
        EventBus.getDefault().register(this);
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

    @Subscribe
    public void onCommandTriggeredEvent(CommandTriggeredEvent receivedEvent){
        Log.d(TAG, "command was triggered...");
    }
}
