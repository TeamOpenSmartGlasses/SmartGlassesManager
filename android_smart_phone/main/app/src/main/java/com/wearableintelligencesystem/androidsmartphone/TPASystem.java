package com.wearableintelligencesystem.androidsmartphone;

import com.wearableintelligencesystem.androidsmartphone.commands.CommandSystem;
import org.greenrobot.eventbus.EventBus;

public class TPASystem {
    private String TAG = "WearableAi_TPACommunicator";
    private CommandSystem commandSystem;

    public TPASystem(){
        EventBus.getDefault().register(this);
        commandSystem = new CommandSystem();
    }

    //    @Subscribe
    //    public void onSendableTranscriptEvent(SendableIntentEvent sendableIntentEvent)
    //    {
    ////        SGMData.TPABroadcastSender.broadcastData(sendableIntentEvent.data);
    //    }

    //    @Subscribe
    //    public void onIntentReceivedEvent(ReceivedIntentEvent receivedIntentEvent) throws JSONException {
    //        Log.d(TAG, "ReceivedIntentData: " + receivedIntentEvent.data);
    //        JSONObject receivedObj = new JSONObject(receivedIntentEvent.data);
    //        String dataType = receivedObj.getString(MessageTypes.MESSAGE_TYPE_LOCAL);
    //        if(dataType.equals(MessageTypes.REFERENCE_CARD_SIMPLE_VIEW))
    //        {
    //            String title = receivedObj.getString(MessageTypes.REFERENCE_CARD_SIMPLE_VIEW_TITLE);
    //            String body = receivedObj.getString(MessageTypes.REFERENCE_CARD_SIMPLE_VIEW_BODY);
    //            //TODO: Once everything is merged, add the call to ActiveLookSGC's displayReferenceCardSimple here
    //            EventBus.getDefault().post(new ReferenceCardSimpleViewRequestEvent(title, body));
    //        }
    //    }
}