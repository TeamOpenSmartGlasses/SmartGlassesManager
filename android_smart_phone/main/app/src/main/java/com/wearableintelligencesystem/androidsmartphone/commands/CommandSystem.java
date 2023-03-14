package com.wearableintelligencesystem.androidsmartphone.commands;

import android.content.Context;
import android.util.Log;

import com.teamopensmartglasses.sgmlib.SGMCallbackMapper;
import com.teamopensmartglasses.sgmlib.SGMCommand;
import com.teamopensmartglasses.sgmlib.SGMCallback;
import com.teamopensmartglasses.sgmlib.events.CommandTriggeredEvent;
import com.teamopensmartglasses.sgmlib.events.ReferenceCardSimpleViewRequestEvent;
import com.teamopensmartglasses.sgmlib.events.RegisterCommandRequestEvent;
import com.wearableintelligencesystem.androidsmartphone.eventbusmessages.StartLiveCaptionsEvent;
import com.wearableintelligencesystem.androidsmartphone.eventbusmessages.StopLiveCaptionsEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class CommandSystem {
    private String TAG = "WearableAi_CommandSystem";

    //hold all registered commands, mostly from TPAs
    public SGMCallbackMapper sgmCallbackMapper;
//    private boolean debugAppRegistered;

    //voice command system
    VoiceCommandServer voiceCommandServer;

    public CommandSystem(Context context){
        sgmCallbackMapper = new SGMCallbackMapper();

        loadDefaultCommands();

        //start voice command server to parse transcript for voice command
        voiceCommandServer = new VoiceCommandServer(context);
        updateInterfaceCommands();

//        debugAppRegistered = false;

        //subscribe to event bus events
        EventBus.getDefault().register(this);
    }

    //TODO: remove this once we have real functionality for default commands
    public void dummyCallback(String args, long commandTime){
        Log.d(TAG, "Default command dummy callback triggered");
    }

    public void launchLiveCaptions(String args, long commandTime){
        EventBus.getDefault().post(new StartLiveCaptionsEvent());
    }

    public void launchTestCard(String args, long commandTime){
        EventBus.getDefault().post(new StopLiveCaptionsEvent());
        EventBus.getDefault().post(new ReferenceCardSimpleViewRequestEvent("Test Card", "This is a test card triggered by voice command."));
    }

    public void loadDefaultCommands(){
        //live life captions - test scrolling view text
        registerCommand("Live Captions", UUID.fromString("933b8950-412e-429e-8fb6-430f973cc9dc"), new String[] { "live captions", "live life captions", "captions", "transcription" }, "Starts streaming captions live to the glasses display.", this::launchLiveCaptions);

        //test reference card
        registerCommand("Test Card", UUID.fromString("f4290426-18d5-431a-aea4-21844b832735"), new String[] { "show me a test card", "test card", "test", "testing", "show test card" }, "Shows a test Reference Card on the glasses display.", this::launchTestCard);

//        //blank screen
//        registerCommand("Blank Screen", UUID.fromString("93401154-b4ab-4166-9aa3-58b79db41ff0"), new String[] { "turn off display", "blank screen" }, "Makes the smart glasses display turn off or go blank.", this::dummyCallback);
    }

    public void registerCommand(String name, UUID id, String[] phrases, String description, SGMCallback callback) {
        //add a new command
        SGMCommand newCommand = new SGMCommand(name, id, phrases, description);
        sgmCallbackMapper.putCommandWithCallback(newCommand, callback);
    }

    private void updateInterfaceCommands(){
        voiceCommandServer.updateVoiceCommands(sgmCallbackMapper.getCommandsList());
    }

    @Subscribe
    public void onRegisterCommandRequestEvent(RegisterCommandRequestEvent receivedEvent){
        sgmCallbackMapper.putCommandWithCallback(receivedEvent.command, null);
        Log.d(TAG, "Command was registered");
        updateInterfaceCommands();

//        //check if the registered command is the debug command
//        if (receivedEvent.command.getId().equals(SGMGlobalConstants.DEBUG_COMMAND_ID)) {
//            debugAppRegistered = true;
//        }
    }
    @Subscribe
    public void onCommandTriggeredEvent(CommandTriggeredEvent receivedEvent){
        Log.d(TAG, "Command was triggered.");
        SGMCommand command = receivedEvent.command;
        String args = receivedEvent.args;
        long commandTriggeredTime = receivedEvent.commandTriggeredTime;
        if (command != null){
            SGMCallback callback = sgmCallbackMapper.getCommandCallback(command);
            if (callback != null){
                callback.runCommand(args, commandTriggeredTime);
            }
        }
    }

    public void destroy(){
        if (voiceCommandServer != null) {
            voiceCommandServer.destroy();
        }

        EventBus.getDefault().unregister(this);
    }

}
