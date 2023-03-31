package com.smartglassesmanager.androidsmartphone;

import com.teamopensmartglasses.sgmlib.events.SpeechRecFinalOutputEvent;
import com.teamopensmartglasses.sgmlib.events.SpeechRecIntermediateOutputEvent;
import com.teamopensmartglasses.sgmlib.events.FinalScrollingTextRequestEvent;
import com.teamopensmartglasses.sgmlib.events.IntermediateScrollingTextRequestEvent;
import com.teamopensmartglasses.sgmlib.events.ScrollingTextViewStartRequestEvent;
import com.smartglassesmanager.androidsmartphone.eventbusmessages.StartLiveCaptionsEvent;
import com.smartglassesmanager.androidsmartphone.eventbusmessages.StopLiveCaptionsEvent;

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
        EventBus.getDefault().post(new ScrollingTextViewStartRequestEvent(title));
    }

    @Subscribe
    public void onStopLiveCaptionsEvent(StopLiveCaptionsEvent receivedEvent){
        active = false;
    }

    @Subscribe
    public void onSpeechRecFinalOutputReceived(SpeechRecFinalOutputEvent receivedEvent){
        if (active) {
            EventBus.getDefault().post(new FinalScrollingTextRequestEvent(receivedEvent.text));
        }
    }

    @Subscribe
    public void onSpeechRecIntermediateOutputReceived(SpeechRecIntermediateOutputEvent receivedEvent){
        if (active) {
            EventBus.getDefault().post(new IntermediateScrollingTextRequestEvent(receivedEvent.text));
        }
    }

    public void destroy(){
        EventBus.getDefault().unregister(this);
    }
}
