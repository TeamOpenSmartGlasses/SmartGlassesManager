package com.teamopensmartglasses.sgmlib;

import android.content.Context;
import android.util.Log;

import com.teamopensmartglasses.sgmlib.events.CommandTriggeredEvent;
import com.teamopensmartglasses.sgmlib.events.ReferenceCardSimpleViewRequestEvent;
import com.teamopensmartglasses.sgmlib.events.RegisterCommandRequestEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class SGMLib {
    public String TAG = "SGMLib_SGMLib";

    private TPABroadcastReceiver sgmReceiver;
    private TPABroadcastSender sgmSender;
    private Context mContext;
    private SGMCallbackMapper sgmCallbackMapper;

    public SGMLib(Context context){
        this.mContext = context;
        sgmCallbackMapper = new SGMCallbackMapper();
        sgmReceiver = new TPABroadcastReceiver(context);
        sgmSender = new TPABroadcastSender(context);

        //register subscribers on EventBus
        EventBus.getDefault().register(this);
    }

    //register a new command
    public void registerCommand(SGMCommand sgmCommand, SGMCallback callback){
        sgmCallbackMapper.putCommandWithCallback(sgmCommand, callback);
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
        SGMCommand command = receivedEvent.command;
        String args = receivedEvent.args;
        long commandTriggeredTime = receivedEvent.commandTriggeredTime;
        Log.d(TAG, " " + command.getId());
        Log.d(TAG, " " + command.getDescription());

        //call the callback
        SGMCallback callback = sgmCallbackMapper.getCommandCallback(command);
        if (callback != null){
            callback.runCommand(args, commandTriggeredTime);
        }
        Log.d(TAG, "Callback called");
    }

    public void deinit(){
        EventBus.getDefault().unregister(this);
        if (sgmReceiver != null) {
            sgmReceiver.destroy();
        }
        if (sgmSender != null) {
            sgmSender.destroy();
        }
    }
}
