package com.wearableintelligencesystem.androidsmartphone.commands;

import com.teamopensmartglasses.sgmlib.SGMCommand;

import java.util.ArrayList;

public abstract class VoiceCommand {
    public String TAG = "WearableAi_VoiceCommand";

    //data about the voice command itself
    protected SGMCommand mCommand;

    //required arguments data
    public boolean requiredArg = false;
    public String requiredArgString;
    public ArrayList<String> requiredArgOptions;

    public VoiceCommand(SGMCommand command){
        this.mCommand = command;
    }

    public boolean runCommand(VoiceCommandServer vcServer, String preArgs, String wakeWord, int command, String postArgs, long commandTime, long transcriptId){
       return true;
    }

    public String getCommandName(){
        return mCommand.getName();
    }

    public ArrayList<String> getPhrases(){
        return mCommand.getPhrases();
    }
}
