package com.teamopensmartglasses.sgmlib;

import static com.teamopensmartglasses.sgmlib.UniversalMessageTypes.EVENT_BUNDLE;
import static com.teamopensmartglasses.sgmlib.UniversalMessageTypes.EVENT_ID;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.teamopensmartglasses.sgmlib.events.ReceivedIntentEvent;
import com.teamopensmartglasses.sgmlib.events.ReferenceCardSimpleViewRequestEvent;
import com.teamopensmartglasses.sgmlib.events.RegisterCommandRequestEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.Serializable;
import java.util.UUID;

public class TPABroadcastReceiver extends BroadcastReceiver {
    private String filterPkg;
    private Context context;
    public String TAG = "HELLO123";

    public TPABroadcastReceiver(Context myContext) {
        this.context = myContext;
        Log.d(TAG, "log: ctxpkgn: " + this.context.getPackageName() + " sgmdat: " + SGMData.SGMPkgName);
        this.filterPkg = this.context.getPackageName().contains(SGMData.SGMPkgName) ? "com.teamopensmartglasses.from3pa" : "com.teamopensmartglasses.to3pa";
        Log.d(TAG, "PKGFILTER: " + this.filterPkg);
        IntentFilter intentFilter = new IntentFilter(this.filterPkg);
        this.context.registerReceiver(this, intentFilter);
        Log.d(TAG, "PKGFILTER: " + this.filterPkg);
    }

    public void onReceive(Context context, Intent intent) {
//        Bundle bundle = intent.getExtras();
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

        for(SGMCommand command : SGMData.registeredCommands){
            if(command.getUUID() == UUID.fromString(eventId)){  //TODO: Incoming EVENT_ID should equal a command's UUID.
              command.getCallback().call();
            }
        }
    }
}
