package com.wearableintelligencesystem.androidsmartphone.commands;

import android.content.Context;
import android.util.Log;

import com.teamopensmartglasses.sgmlib.SGMCommand;

import java.util.ArrayList;
import java.util.UUID;

public class CommandSystem {
    private String TAG = "WearableAi_CommandSystem";

    //hold all registered commands, mostly from TPAs
    ArrayList<SGMCommand> allCommands;

    //voice command system
    VoiceCommandServer voiceCommandServer;

    public CommandSystem(Context context){
        allCommands = new ArrayList<>();
        loadDefaultCommands();

        //start voice command server to parse transcript for voice command
        voiceCommandServer = new VoiceCommandServer(context);
    }

    private void loadDefaultCommands(){
        //live life captions - test scrolling view text
//        registerCommand("Live Captions", UUID.fromString("933b8950-412e-429e-8fb6-430f973cc9dc"), new String[] { "life captions", "live life captions", "captions", "transcription" }, "Starts streaming captions live to the glasses display.", this::dummyCallback);
//
//        //test reference card
//        registerCommand("Test Card", UUID.fromString("f4290426-18d5-431a-aea4-21844b832735"), new String[] { "show me a test card", "test card", "test", "testing", "show test card" }, "Shows a test Reference Card on the glasses display.", this::dummyCallback);
//
//        //blank screen
//        registerCommand("Blank Screen", UUID.fromString("93401154-b4ab-4166-9aa3-58b79db41ff0"), new String[] { "turn off display", "blank screen" }, "Makes the smart glasses display turn off or go blank.", this::dummyCallback);
    }

    //TODO: remove this once we have real functionality for default commands
    public void dummyCallback(){
        Log.d(TAG, "Default command dummy callback triggered");
    }

    public void registerCommand(String name, UUID id, String[] phrases, String description) {
        //add a new command
        SGMCommand newCommand = new SGMCommand(name, id, phrases, description);
        allCommands.add(newCommand);
    }

    public void registerCommand(SGMCommand command)
    {
        allCommands.add(command);
    }
}
