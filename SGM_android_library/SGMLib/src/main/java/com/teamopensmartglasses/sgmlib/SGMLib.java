package com.teamopensmartglasses.sgmlib;

import android.content.Context;
import android.util.Log;

import com.teamopensmartglasses.sgmlib.events.CommandTriggeredEvent;
import com.teamopensmartglasses.sgmlib.events.FinalScrollingTextEvent;
import com.teamopensmartglasses.sgmlib.events.ReferenceCardSimpleViewRequestEvent;
import com.teamopensmartglasses.sgmlib.events.RegisterCommandRequestEvent;
import com.teamopensmartglasses.sgmlib.events.ScrollingTextViewStartEvent;
import com.teamopensmartglasses.sgmlib.events.ScrollingTextViewStopEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class SGMLib {
    public String TAG = "SGMLib_SGMLib";

    public TPABroadcastReceiver sgmReceiver;
    public TPABroadcastSender sgmSender;
    public Context mContext;
    public HashMap<UUID, SGMCommandWithCallback> registeredCommands;

    public SGMLib(Context context){
        this.mContext = context;
        sgmReceiver = new TPABroadcastReceiver(context);
        sgmSender = new TPABroadcastSender(context);
        registeredCommands = new HashMap<>();

        //register subscribers on EventBus
        EventBus.getDefault().register(this);
    }

    //register a new command
    public void registerCommand(SGMCommand sgmCommand, Callback callback){
        registeredCommands.put(sgmCommand.getId(), new SGMCommandWithCallback(sgmCommand, callback));
        EventBus.getDefault().post(new RegisterCommandRequestEvent(sgmCommand));
    }

    //register our app with the SGM
    public void registerApp(String appName, String appDescription) {
    }

    //show a reference card on the smart glasses with title and body text
    public void sendReferenceCard(String title, String body) {
        EventBus.getDefault().post(new ReferenceCardSimpleViewRequestEvent(title, body));
    }

    public void startScrollingText(String title){
        EventBus.getDefault().post(new ScrollingTextViewStartEvent(title));
    }

    public void pushScrollingText(String text){
        EventBus.getDefault().post(new FinalScrollingTextEvent(text));
    }
    public void stopScrollingText(){
        EventBus.getDefault().post(new ScrollingTextViewStopEvent());
    }

    @Subscribe
    public void onCommandTriggeredEvent(CommandTriggeredEvent receivedEvent){
        SGMCommand command = receivedEvent.command;
        Log.d(TAG, " " + command.getId());
        Log.d(TAG, " " + command.getDescription());
        //call the callback
        registeredCommands.get(command.getId()).callback.call();
        Log.d(TAG, "Callback called");
    }
}
