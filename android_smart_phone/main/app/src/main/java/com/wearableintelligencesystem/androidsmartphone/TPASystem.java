package com.wearableintelligencesystem.androidsmartphone;

import android.content.Context;
import android.util.Log;

import com.teamopensmartglasses.sgmlib.SGMCommand;
import com.teamopensmartglasses.sgmlib.events.CommandTriggeredEvent;
import com.teamopensmartglasses.sgmlib.events.RegisterCommandRequestEvent;
import com.teamopensmartglasses.sgmlib.events.SpeechRecFinalOutputEvent;
import com.teamopensmartglasses.sgmlib.events.SpeechRecIntermediateOutputEvent;
import com.teamopensmartglasses.sgmlib.events.SubscribeDataStreamRequestEvent;
import com.wearableintelligencesystem.androidsmartphone.commands.CommandSystem;
import com.wearableintelligencesystem.androidsmartphone.comms.SGMLibBroadcastReceiver;
import com.wearableintelligencesystem.androidsmartphone.comms.SGMLibBroadcastSender;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.HashMap;
import java.util.UUID;

public class TPASystem {
    private String TAG = "WearableAi_TPASystem";
    private Context mContext;
    private SGMLibBroadcastSender sgmLibBroadcastSender;
    private SGMLibBroadcastReceiver sgmLibBroadcastReceiver;

    public TPASystem(Context context){
        mContext = context;
        sgmLibBroadcastSender = new SGMLibBroadcastSender(mContext);
        sgmLibBroadcastReceiver = new SGMLibBroadcastReceiver(mContext);

        //subscribe to event bus events
        EventBus.getDefault().register(this);
    }

    @Subscribe
    public void onCommandTriggeredEvent(CommandTriggeredEvent receivedEvent){
        Log.d(TAG, "Command was triggered.");
        SGMCommand command = receivedEvent.command;
        String args = receivedEvent.args;
        long commandTriggeredTime = receivedEvent.commandTriggeredTime;
        if (command != null){
            sgmLibBroadcastSender.sendEventToTPAs(CommandTriggeredEvent.eventId, new CommandTriggeredEvent(command, args, commandTriggeredTime));
            //sgmLibBroadcastSender.sendEventToTPAs(CommandTriggeredEvent.eventId, new CommandTriggeredEvent(command));
        }
    }

    @Subscribe
    public void onIntermediateTranscript(SpeechRecIntermediateOutputEvent event){
        Log.d(TAG, "Intermediate Transcript Sending Event");
        boolean tpaIsSubscribed = true; //TODO: Hash out implementation
        if(tpaIsSubscribed){
            sgmLibBroadcastSender.sendEventToTPAs(SpeechRecIntermediateOutputEvent.eventId, event);
        }
    }

    @Subscribe
    public void onFinalTranscript(SpeechRecFinalOutputEvent event){
     Log.d(TAG, "Final Transcript Sending Event");
        boolean tpaIsSubscribed = true; //TODO: Hash out implementation
        if(tpaIsSubscribed){
            sgmLibBroadcastSender.sendEventToTPAs(SpeechRecFinalOutputEvent.eventId, event);
        }
    }

    @Subscribe
    public void onRegisterCommandRequestEvent(RegisterCommandRequestEvent receivedEvent){
        registeredCommands.put(receivedEvent.command.getId(), receivedEvent.command);
        Log.d(TAG, "Command was registered");

        //check if the registered command is the debug command
        if (receivedEvent.command.getId().equals(SGMGlobalConstants.DEBUG_COMMAND_ID)) {
            debugAppRegistered = true;
        }
    }

    @Subscribe
    public void onSubscribeDataStreamRequestEvent(SubscribeDataStreamRequestEvent event){
        Log.d(TAG, "Got a request to subscribe to data stream");
        /*
            TODO: Hash out implementation
            Should data stream subscriptions use an SGMCommand for its callback function,
            or something else?
        */
    }

    public void destroy(){
        sgmLibBroadcastReceiver.unregister();
        EventBus.getDefault().unregister(this);
    }
}