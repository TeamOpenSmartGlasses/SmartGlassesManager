package com.teamopensmartglasses.sgmlib;

import static com.teamopensmartglasses.sgmlib.GlobalStrings.EVENT_BUNDLE;
import static com.teamopensmartglasses.sgmlib.GlobalStrings.EVENT_ID;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import com.teamopensmartglasses.sgmlib.events.ReferenceCardSimpleViewRequestEvent;
import com.teamopensmartglasses.sgmlib.events.RegisterCommandRequestEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.Serializable;

public class TPABroadcastSender {
    private String intentPkg;
    Context context;

    public TPABroadcastSender(Context context) {
        this.context = context;
        this.intentPkg = "com.teamopensmartglasses.from3pa";

        //register event bus subscribers
        EventBus.getDefault().register(this);
    }

    public void sendEventToSGM(String eventId, Serializable eventBundle) {
        Log.d("3PASEND event: ", this.intentPkg);

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
    public void onReferenceCardSimpleViewEvent(ReferenceCardSimpleViewRequestEvent receivedEvent){
        String eventId = receivedEvent.getEventId();
        sendEventToSGM(eventId, receivedEvent);
    }

    @Subscribe
    public void onRegisterCommandRequestEvent(RegisterCommandRequestEvent receivedEvent){
        String eventId = receivedEvent.getEventId();
        sendEventToSGM(eventId, receivedEvent);
    }
}