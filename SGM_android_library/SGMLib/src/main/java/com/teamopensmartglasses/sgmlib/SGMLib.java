package com.teamopensmartglasses.sgmlib;

import android.content.Context;
import android.util.Log;

import com.teamopensmartglasses.sgmlib.events.CommandTriggeredEvent;
import com.teamopensmartglasses.sgmlib.events.ReferenceCardSimpleViewRequestEvent;
import com.teamopensmartglasses.sgmlib.events.RegisterCommandRequestEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

public class SGMLib {
    public String TAG = "SGMLib_SGMLib";

    public TPABroadcastReceiver sgmReceiver;
    public TPABroadcastSender sgmSender;
    public Context mContext;
    public ArrayList<SGMCommand> registeredCommands;

    public SGMLib(Context context){
        this.mContext = context;
        sgmReceiver = new TPABroadcastReceiver(context);
        sgmSender = new TPABroadcastSender(context);
        registeredCommands = new ArrayList<>();

        //register subscribers on EventBus
        EventBus.getDefault().register(this);
    }

    //register a new command
    public void registerCommand(SGMCommand sgmCommand){
        registeredCommands.add(sgmCommand);
        EventBus.getDefault().post(new RegisterCommandRequestEvent(sgmCommand));
    }

    //register our app with the SGM
    public void registerApp(String appName, String appDescription) {
    }

    //show a reference card on the smart glasses with title and body text
    public void sendReferenceCard(String title, String body) {
        EventBus.getDefault().post(new ReferenceCardSimpleViewRequestEvent(title, body));
    }

    @Subscribe
    public void onCommandTriggeredEvent(CommandTriggeredEvent receivedEvent){
        Log.d(TAG, "Callback called");
        SGMCommand command = receivedEvent.command;
        command.getCallback();
    }
}
