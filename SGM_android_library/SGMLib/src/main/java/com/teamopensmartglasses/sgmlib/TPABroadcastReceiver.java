package com.teamopensmartglasses.sgmlib;

import static android.content.Context.RECEIVER_EXPORTED;
import static com.teamopensmartglasses.sgmlib.SGMGlobalConstants.EVENT_BUNDLE;
import static com.teamopensmartglasses.sgmlib.SGMGlobalConstants.EVENT_ID;
import static com.teamopensmartglasses.sgmlib.SGMGlobalConstants.SGMPkgName;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.teamopensmartglasses.sgmlib.events.CommandTriggeredEvent;
import com.teamopensmartglasses.sgmlib.events.FocusChangedEvent;
import com.teamopensmartglasses.sgmlib.events.FocusRequestEvent;
import com.teamopensmartglasses.sgmlib.events.GlassesTapOutputEvent;
import com.teamopensmartglasses.sgmlib.events.KillTpaEvent;
import com.teamopensmartglasses.sgmlib.events.SmartRingButtonOutputEvent;
import com.teamopensmartglasses.sgmlib.events.SpeechRecFinalOutputEvent;
import com.teamopensmartglasses.sgmlib.events.SpeechRecIntermediateOutputEvent;

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
        this.context.registerReceiver(this, intentFilter, RECEIVER_EXPORTED);
    }

    public void onReceive(Context context, Intent intent) {
        String eventId = intent.getStringExtra(EVENT_ID);
        Serializable serializedEvent = intent.getSerializableExtra(EVENT_BUNDLE);

        //map from id to event
        switch (eventId) {
            case CommandTriggeredEvent.eventId:
                EventBus.getDefault().post((CommandTriggeredEvent) serializedEvent);
                break;
            case KillTpaEvent.eventId:
                EventBus.getDefault().post((KillTpaEvent) serializedEvent);
                break;
            case SpeechRecIntermediateOutputEvent.eventId:
                EventBus.getDefault().post((SpeechRecIntermediateOutputEvent) serializedEvent);
                break;
            case SpeechRecFinalOutputEvent.eventId:
                EventBus.getDefault().post((SpeechRecFinalOutputEvent) serializedEvent);
                break;
            case SmartRingButtonOutputEvent.eventId:
                EventBus.getDefault().post((SmartRingButtonOutputEvent) serializedEvent);
                break;
            case GlassesTapOutputEvent.eventId:
                EventBus.getDefault().post((GlassesTapOutputEvent) serializedEvent);
                break;
            case FocusChangedEvent.eventId:
                EventBus.getDefault().post((FocusChangedEvent) serializedEvent);
                break;
        }
    }

    public void destroy(){
        this.context.unregisterReceiver(this);
    }
}
