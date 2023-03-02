package com.wearableintelligencesystem.androidsmartphone.comms;

import static com.teamopensmartglasses.sgmlib.GlobalStrings.EVENT_BUNDLE;
import static com.teamopensmartglasses.sgmlib.GlobalStrings.EVENT_ID;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.teamopensmartglasses.sgmlib.SGMCommand;
import com.teamopensmartglasses.sgmlib.events.ReferenceCardSimpleViewRequestEvent;
import com.teamopensmartglasses.sgmlib.events.RegisterCommandRequestEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.Serializable;

public class SGMLibBroadcastReceiver extends BroadcastReceiver {
    private String filterPkg;
    private Context context;
    public String TAG = "WearableAi_SGMLibBroadcastReceiver";

    public SGMLibBroadcastReceiver(Context myContext) {
        this.context = myContext;
        this.filterPkg = "com.teamopensmartglasses.from3pa";
        IntentFilter intentFilter = new IntentFilter(this.filterPkg);
        this.context.registerReceiver(this, intentFilter);
    }

    public void onReceive(Context context, Intent intent) {
        String eventId = intent.getStringExtra(EVENT_ID);

        Log.d(TAG, "GOT EVENT ID: " + eventId);
        Serializable serializedEvent = intent.getSerializableExtra(EVENT_BUNDLE);
        Log.i("Broadcastreceiver", "BroadcastReceiver Received");

        //map from id to event
        if (eventId.equals(ReferenceCardSimpleViewRequestEvent.getEventId())){
            Log.d(TAG, "Sending Reference Card event");
            EventBus.getDefault().post((ReferenceCardSimpleViewRequestEvent) serializedEvent);
        }

        //TODO: find place to subscribe to this
        if (eventId.equals(RegisterCommandRequestEvent.getEventId()))
        {
            Log.d(TAG, "Got register command event");
            EventBus.getDefault().post((RegisterCommandRequestEvent) serializedEvent);
        }

//        for(SGMCommand command : sgmData.registeredCommands){
//            if(command.getUUID() == UUID.fromString(eventId)){
//              command.getCallback().call();
//            }
//        }
    }
}

