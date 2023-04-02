package com.teamopensmartglasses.sgmlib;

import static com.teamopensmartglasses.sgmlib.SGMGlobalConstants.APP_PKG_NAME;
import static com.teamopensmartglasses.sgmlib.SGMGlobalConstants.EVENT_BUNDLE;
import static com.teamopensmartglasses.sgmlib.SGMGlobalConstants.EVENT_ID;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.teamopensmartglasses.sgmlib.events.FinalScrollingTextRequestEvent;
import com.teamopensmartglasses.sgmlib.events.FocusRequestEvent;
import com.teamopensmartglasses.sgmlib.events.ReferenceCardSimpleViewRequestEvent;
import com.teamopensmartglasses.sgmlib.events.RegisterCommandRequestEvent;
import com.teamopensmartglasses.sgmlib.events.ScrollingTextViewStartRequestEvent;
import com.teamopensmartglasses.sgmlib.events.ScrollingTextViewStopRequestEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.Serializable;

public class TPABroadcastSender {
    private String intentPkg;
    private String packageName;
    Context context;

    public TPABroadcastSender(Context context) {
        this.context = context;
        this.intentPkg = SGMGlobalConstants.FROM_TPA_FILTER;
        packageName = context.getPackageName();

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
        intent.putExtra(APP_PKG_NAME, packageName);
        intent.putExtra(EVENT_BUNDLE, eventBundle);
        context.sendBroadcast(intent);
    }

    @Subscribe
    public void onReferenceCardSimpleViewEvent(ReferenceCardSimpleViewRequestEvent receivedEvent){
        String eventId = receivedEvent.eventId;
        sendEventToSGM(eventId, receivedEvent);
    }

    @Subscribe
    public void onStartScrollingTextEvent(ScrollingTextViewStartRequestEvent receivedEvent){
        String eventId = receivedEvent.eventId;
        sendEventToSGM(eventId, receivedEvent);
    }

    @Subscribe
    public void onFinalScrollingTextEvent(FinalScrollingTextRequestEvent receivedEvent){
        Log.d("TPASEND", "FINAL SCROLL SEND");
        String eventId = receivedEvent.eventId;
        sendEventToSGM(eventId, receivedEvent);
    }
    public void onScrollingTextViewStopEvent(ScrollingTextViewStopRequestEvent receivedEvent){
        String eventId = receivedEvent.eventId;
        sendEventToSGM(eventId, receivedEvent);
    }

    @Subscribe
    public void onRegisterCommandRequestEvent(RegisterCommandRequestEvent receivedEvent){
        String eventId = receivedEvent.getEventId();
        sendEventToSGM(eventId, receivedEvent);
    }

    @Subscribe
    public void onFocusRequestEvent(FocusRequestEvent receivedEvent){
        String eventId = receivedEvent.eventId;
        sendEventToSGM(eventId, receivedEvent);
    }

    public void destroy(){
        //unregister event bus subscribers
        EventBus.getDefault().unregister(this);
    }
}