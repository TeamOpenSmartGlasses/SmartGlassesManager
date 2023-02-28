package com.teamopensmartglasses.sgmlib;

import static com.teamopensmartglasses.sgmlib.UniversalMessageTypes.EVENT_BUNDLE;
import static com.teamopensmartglasses.sgmlib.UniversalMessageTypes.EVENT_CLASS;
import static com.teamopensmartglasses.sgmlib.UniversalMessageTypes.EVENT_ID;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
        this.intentPkg = this.context.getPackageName().contains(SGMData.SGMPkgName) ? "com.teamopensmartglasses.to3pa" : "com.teamopensmartglasses.from3pa";
        SGMData.TPABroadcastSender = this;

        //register event bus subscribers
        EventBus.getDefault().register(this);
    }

    public void broadcastData(String data) {
        Log.d("3PASEND: ", this.intentPkg);
        Intent intent = new Intent();
        intent.putExtra("data", data);
        intent.setAction(intentPkg);
        intent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        context.sendBroadcast(intent);
    }

    public void sendEventToSGM(String eventId, Serializable eventBundle) {
        Log.d("3PASEND event: ", this.intentPkg);

        //setup intent to send
        Intent intent = new Intent();
        intent.setAction(intentPkg);
        intent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

        //load in our data
        intent.putExtra(EVENT_ID, eventId);
        intent.putExtra(EVENT_BUNDLE, eventBundle);

        //send it
        context.sendBroadcast(intent);
    }

    @Subscribe
    public void onReferenceCardSimpleViewEvent(ReferenceCardSimpleViewRequestEvent receivedEvent){
        String eventId = receivedEvent.getEventId();
//        Bundle eventBundle = new Bundle();
//        eventBundle.putSerializable(EVENT_CLASS, receivedEvent);
        sendEventToSGM(eventId, receivedEvent);
    }

    @Subscribe
    public void onRegisterCommandRequestEvent(RegisterCommandRequestEvent receivedEvent){
        String eventId = receivedEvent.getEventId();
        sendEventToSGM(eventId, receivedEvent);
    }
}