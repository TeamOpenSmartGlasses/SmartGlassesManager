package com.wearableintelligencesystem.androidsmartphone.comms;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import SGMLib.SGMData;

public class TPACommunicator {
    public TPACommunicator(){
        EventBus.getDefault().register(this);
    }

    @Subscribe
    public void onSendableTranscriptEvent(SendableTranscriptEvent sendableTranscriptEvent)
    {
        SGMData.sgmBroadcastSender.broadcastData(sendableTranscriptEvent.data);
    }
}
