package com.teamopensmartglasses.sgmlib;

import static com.teamopensmartglasses.sgmlib.SGMGlobalConstants.EVENT_BUNDLE;
import static com.teamopensmartglasses.sgmlib.SGMGlobalConstants.EVENT_ID;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.teamopensmartglasses.sgmlib.events.CommandTriggeredEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.Serializable;

public class TPABroadcastReceiver extends BroadcastReceiver {
    private String filterPkg;
    private Context context;
    public String TAG = "SGMLib_TPABroadcastReceiver";

    public TPABroadcastReceiver(Context myContext) {
        this.context = myContext;
        this.filterPkg = SGMGlobalConstants.TO_TPA_FILTER;
        IntentFilter intentFilter = new IntentFilter(this.filterPkg);
        this.context.registerReceiver(this, intentFilter);
    }

    public void onReceive(Context context, Intent intent) {
        String eventId = intent.getStringExtra(EVENT_ID);
        Serializable serializedEvent = intent.getSerializableExtra(EVENT_BUNDLE);
        Log.d(TAG, "GOT EVENT ID: " + eventId);
        Log.i("Broadcastreceiver", "BroadcastReceiver Received");

        //map from id to event
        switch (eventId) {
            case CommandTriggeredEvent.eventId:
                Log.d(TAG, "Resending Command triggered event");
                EventBus.getDefault().post((CommandTriggeredEvent) serializedEvent);
                break;
        }
    }
}
