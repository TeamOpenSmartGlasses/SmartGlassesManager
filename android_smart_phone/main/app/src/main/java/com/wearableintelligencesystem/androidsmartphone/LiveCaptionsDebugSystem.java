package com.wearableintelligencesystem.androidsmartphone;

import android.util.Log;

import com.teamopensmartglasses.sgmlib.events.SpeechRecFinalOutputEvent;
import com.teamopensmartglasses.sgmlib.events.SpeechRecIntermediateOutputEvent;
import com.teamopensmartglasses.sgmlib.events.FinalScrollingTextEvent;
import com.teamopensmartglasses.sgmlib.events.IntermediateScrollingTextEvent;
import com.teamopensmartglasses.sgmlib.events.ScrollingTextViewStartEvent;
import com.wearableintelligencesystem.androidsmartphone.eventbusmessages.StartLiveCaptionsEvent;
import com.wearableintelligencesystem.androidsmartphone.eventbusmessages.StopLiveCaptionsEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

//uses scrolling text view to display live captions
public class LiveCaptionsDebugSystem {
    private  final String TAG = "WearableAi_LiveCaptionsDebugSystem";

    private String title;
    private boolean active;

    public LiveCaptionsDebugSystem() {
        title = "Live Captions";
        active = false;
        EventBus.getDefault().register(this);
    }

    @Subscribe
    public void onStartLiveCaptionsEvent(StartLiveCaptionsEvent receivedEvent){
        active = true;
        //start the mode on the glasses, using scrolling text view
        EventBus.getDefault().post(new ScrollingTextViewStartEvent(title));
    }

    @Subscribe
    public void onStopLiveCaptionsEvent(StopLiveCaptionsEvent receivedEvent){
        active = false;
    }

    @Subscribe
    public void onSpeechRecFinalOutputReceived(SpeechRecFinalOutputEvent receivedEvent){
        Log.d(TAG, "onFinalOutputReceived");
        if (active) {
            Log.d(TAG, "POSTING IT THO");
            EventBus.getDefault().post(new FinalScrollingTextEvent(receivedEvent.text));
        }
    }

    @Subscribe
    public void onSpeechRecIntermediateOutputReceived(SpeechRecIntermediateOutputEvent receivedEvent){
        if (active) {
            EventBus.getDefault().post(new IntermediateScrollingTextEvent(receivedEvent.text));
        }
    }

    public void destroy(){
        EventBus.getDefault().unregister(this);
    }
}
