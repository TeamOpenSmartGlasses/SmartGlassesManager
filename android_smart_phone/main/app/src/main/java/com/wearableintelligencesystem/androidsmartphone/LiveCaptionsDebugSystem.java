package com.wearableintelligencesystem.androidsmartphone;

import com.wearableintelligencesystem.androidsmartphone.eventbusmessages.AudioChunkNewEvent;
import com.wearableintelligencesystem.androidsmartphone.eventbusmessages.FinalScrollingTextEvent;
import com.wearableintelligencesystem.androidsmartphone.eventbusmessages.ScrollingTextViewStartEvent;
import com.wearableintelligencesystem.androidsmartphone.eventbusmessages.SpeechRecFinalOutputEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class LiveCaptionsDebugSystem {
    private String title = "Live Captions";

    public LiveCaptionsDebugSystem(){
        EventBus.getDefault().register(this);

        //start the scrolling mode
        EventBus.getDefault().post(new ScrollingTextViewStartEvent(title));
    }

    @Subscribe
    public void onSpeechRecFinalOutputReceived(SpeechRecFinalOutputEvent receivedEvent){
        EventBus.getDefault().post(new FinalScrollingTextEvent(receivedEvent.text));
    }

    public void destroy(){
        EventBus.getDefault().unregister(this);
    }
}
