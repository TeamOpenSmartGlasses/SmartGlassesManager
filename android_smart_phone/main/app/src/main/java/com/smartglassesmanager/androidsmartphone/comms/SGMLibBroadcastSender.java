package com.smartglassesmanager.androidsmartphone.comms;

import static com.teamopensmartglasses.sgmlib.SGMGlobalConstants.EVENT_BUNDLE;
import static com.teamopensmartglasses.sgmlib.SGMGlobalConstants.EVENT_ID;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.teamopensmartglasses.sgmlib.SGMCommand;
import com.teamopensmartglasses.sgmlib.SGMGlobalConstants;
import com.teamopensmartglasses.sgmlib.SmartGlassesAndroidService;
import com.teamopensmartglasses.sgmlib.events.CommandTriggeredEvent;

import java.io.Serializable;

public class SGMLibBroadcastSender {
    private String TAG = "WearableAi_SGMLibBroadcastSEnder";
    private String intentPkg;
    Context context;

    public SGMLibBroadcastSender (Context context) {
        this.context = context;
        this.intentPkg = SGMGlobalConstants.TO_TPA_FILTER;
    }

    public void sendEventToTPAs(String eventId, Serializable eventBundle) {
        sendEventToTPAs(eventId, eventBundle, null);
    }
    public void sendEventToTPAs(String eventId, Serializable eventBundle, String tpaPackageName) {
        //If we're triggering a command, make sure the command's respective service is running
        if(eventId == CommandTriggeredEvent.eventId){
            SGMCommand cmd = ((CommandTriggeredEvent)eventBundle).command;
            startSgmCommandService(cmd);
        }

        //setup intent to send
        Intent intent = new Intent();
        intent.setAction(intentPkg);
        if (tpaPackageName != null) {
            intent.setPackage(tpaPackageName);
        }
        intent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

        //load in and send data
        intent.putExtra(EVENT_ID, eventId);
        intent.putExtra(EVENT_BUNDLE, eventBundle);
        context.sendBroadcast(intent);
    }

    //Starts a SGMCommand's service (if not already running)
    public void startSgmCommandService(SGMCommand sgmCommand){
        //tpaPackageName = "com.google.mlkit.samples.nl.translate";
        //tpaServiceName = ".java.TranslationService";
        Log.d(TAG, "Starting command service: " + sgmCommand.packageName);

        if(sgmCommand.getPackageName() == "" || sgmCommand.getServiceName() == "") return;

        Intent i = new Intent();
        i.setAction("SGM_COMMAND_INTENT");
        i.putExtra(SmartGlassesAndroidService.TPA_ACTION, SmartGlassesAndroidService.ACTION_START_FOREGROUND_SERVICE);
        i.setComponent(new ComponentName(sgmCommand.packageName, sgmCommand.serviceName));
        ComponentName c = context.startForegroundService(i);
    }
}
