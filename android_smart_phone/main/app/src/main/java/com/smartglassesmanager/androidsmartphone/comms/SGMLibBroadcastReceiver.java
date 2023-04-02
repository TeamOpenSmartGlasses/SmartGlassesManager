package com.smartglassesmanager.androidsmartphone.comms;

import static com.teamopensmartglasses.sgmlib.SGMGlobalConstants.APP_PKG_NAME;
import static com.teamopensmartglasses.sgmlib.SGMGlobalConstants.EVENT_BUNDLE;
import static com.teamopensmartglasses.sgmlib.SGMGlobalConstants.EVENT_ID;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.smartglassesmanager.androidsmartphone.eventbusmessages.TPARequestEvent;
import com.teamopensmartglasses.sgmlib.SGMGlobalConstants;
import com.teamopensmartglasses.sgmlib.events.FinalScrollingTextRequestEvent;
import com.teamopensmartglasses.sgmlib.events.FocusChangedEvent;
import com.teamopensmartglasses.sgmlib.events.FocusRequestEvent;
import com.teamopensmartglasses.sgmlib.events.ReferenceCardSimpleViewRequestEvent;
import com.teamopensmartglasses.sgmlib.events.RegisterCommandRequestEvent;
import com.teamopensmartglasses.sgmlib.events.ScrollingTextViewStartRequestEvent;
import com.teamopensmartglasses.sgmlib.events.ScrollingTextViewStopRequestEvent;
import com.teamopensmartglasses.sgmlib.events.SubscribeDataStreamRequestEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.Serializable;

public class SGMLibBroadcastReceiver extends BroadcastReceiver {
    private String filterPkg;
    private Context context;
    public String TAG = "WearableAi_SGMLibBroadcastReceiver";

    public SGMLibBroadcastReceiver() {

    }

    public SGMLibBroadcastReceiver(Context context) {
        this.context = context;
        this.filterPkg = SGMGlobalConstants.FROM_TPA_FILTER;
        IntentFilter intentFilter = new IntentFilter(this.filterPkg);
        context.registerReceiver(this, intentFilter);
    }

    public void onReceive(Context context, Intent intent) {
        String eventId = intent.getStringExtra(EVENT_ID);
        String sendingPackage = intent.getStringExtra(APP_PKG_NAME);
        Serializable serializedEvent = intent.getSerializableExtra(EVENT_BUNDLE);
        Log.d(TAG, "GOT EVENT ID: " + eventId);

        //map from id to event
        switch (eventId) {
            //if it's a request to run something on glasses or anything else having to do with commands, pipe this through the command system
            case ReferenceCardSimpleViewRequestEvent.eventId:
            case ScrollingTextViewStartRequestEvent.eventId:
            case ScrollingTextViewStopRequestEvent.eventId:
            case FinalScrollingTextRequestEvent.eventId:
            case RegisterCommandRequestEvent.eventId:
            case FocusRequestEvent.eventId:
                Log.d(TAG, "Piping command event to CommandSystem for verification before broadcast.");
                EventBus.getDefault().post(new TPARequestEvent(eventId, serializedEvent, sendingPackage));
                break;
            case SubscribeDataStreamRequestEvent.eventId:
                Log.d(TAG, "Resending subscribe to data stream request event");
                EventBus.getDefault().post((SubscribeDataStreamRequestEvent) serializedEvent);
                break;
        }
    }

    public void unregister(){
       context.unregisterReceiver(this);
    }
}

