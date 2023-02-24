package com.wearableintelligencesystem.androidsmartphone.comms;

import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import com.teamopensmartglasses.sgmlib.events.ReceivedIntentEvent;
import com.teamopensmartglasses.sgmlib.SGMData;
import com.teamopensmartglasses.sgmlib.events.SendableIntentEvent;
import com.teamopensmartglasses.sgmlib.events.ReferenceCardSimpleViewRequestEvent;

public class TPACommunicator {
    private String TAG = "TPACommunicator: ";
    public TPACommunicator(){
        EventBus.getDefault().register(this);
    }

    @Subscribe
    public void onSendableTranscriptEvent(SendableIntentEvent sendableIntentEvent)
    {
        SGMData.TPABroadcastSender.broadcastData(sendableIntentEvent.data);
    }

    @Subscribe
    public void onIntentReceivedEvent(ReceivedIntentEvent receivedIntentEvent) throws JSONException {
        Log.d(TAG, "ReceivedIntentData: " + receivedIntentEvent.data);
        JSONObject receivedObj = new JSONObject(receivedIntentEvent.data);
        String dataType = receivedObj.getString(MessageTypes.MESSAGE_TYPE_LOCAL);
        if(dataType.equals(MessageTypes.REFERENCE_CARD_SIMPLE_VIEW))
        {
            String title = receivedObj.getString(MessageTypes.REFERENCE_CARD_SIMPLE_VIEW_TITLE);
            String body = receivedObj.getString(MessageTypes.REFERENCE_CARD_SIMPLE_VIEW_BODY);
            //TODO: Once everything is merged, add the call to ActiveLookSGC's displayReferenceCardSimple here
            EventBus.getDefault().post(new ReferenceCardSimpleViewRequestEvent(title, body));
        }
    }
}
