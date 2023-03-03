package com.wearableintelligencesystem.androidsmartphone;

import android.content.Context;
import android.util.Log;

import com.teamopensmartglasses.sgmlib.SGMCommand;
import com.teamopensmartglasses.sgmlib.TPABroadcastReceiver;
import com.teamopensmartglasses.sgmlib.TPABroadcastSender;
import com.teamopensmartglasses.sgmlib.events.CommandTriggeredEvent;
import com.teamopensmartglasses.sgmlib.events.RegisterCommandRequestEvent;
import com.wearableintelligencesystem.androidsmartphone.commands.CommandSystem;
import com.wearableintelligencesystem.androidsmartphone.comms.SGMLibBroadcastReceiver;
import com.wearableintelligencesystem.androidsmartphone.comms.SGMLibBroadcastSender;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

public class TPASystem {
    private String TAG = "WearableAi_TPACommunicator";
    private Context mContext;
    private CommandSystem commandSystem;
    private SGMLibBroadcastSender sgmLibBroadcastSender;
    private SGMLibBroadcastReceiver sgmLibBroadcastReceiver;
    public ArrayList<SGMCommand> registeredCommands;

    public TPASystem(Context context){
        mContext = context;
        commandSystem = new CommandSystem();
        sgmLibBroadcastSender = new SGMLibBroadcastSender(mContext);
        sgmLibBroadcastReceiver = new SGMLibBroadcastReceiver(mContext);
        registeredCommands = new ArrayList<>();

        EventBus.getDefault().register(this);
    }

    @Subscribe
    public void onCommandTriggeredEvent(CommandTriggeredEvent receivedEvent){
        Log.d(TAG, "command was triggered...");
    }

    @Subscribe
    public void onRegisterCommandRequestEvent(RegisterCommandRequestEvent receivedEvent){
        registeredCommands.add(receivedEvent.command);
        Log.d(TAG, "Command was registered");
    }

    public void destroy(){
        sgmLibBroadcastReceiver.unregister();
    }
}