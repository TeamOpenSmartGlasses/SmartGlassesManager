package com.wearableintelligencesystem.androidsmartphone.voicecommand;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.wearableintelligencesystem.androidsmartphone.comms.MessageTypes;
import com.wearableintelligencesystem.androidsmartphone.database.voicecommand.VoiceCommandCreator;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

class SelectVoiceCommand extends VoiceCommand {
    private String TAG = "WearableAi_SelectVoiceCommand";

    SelectVoiceCommand(Context context){
        super(context);
        this.commandName = "select";
        this.commandList = new ArrayList<>(Arrays.asList(new String [] {"select"}));
        this.wakeWordList = new ArrayList<>(Arrays.asList(new String [] {}));
        this.isPrimary = false; //not a primary command that can be run at any time
    }

    @Override
    public boolean runCommand(VoiceCommandServer vcServer, String preArgs, String wakeWord, int command, String postArgs, long commandTime, long transcriptId){
        Log.d(TAG, "Running command: " + this.commandList.get(command));

        //parse command
        String commandSpoken = this.commandList.get(command);

        //save master command
        VoiceCommandCreator.create(this.commandName, commandSpoken, wakeWord, true, null, null, commandTime, preArgs, postArgs,"asg_transcript", transcriptId, vcServer.mVoiceCommandRepository);

        //parse the argument to find which mode they want to switch to
        Log.d(TAG, "postArgs is: " + postArgs);
        String selection = this.getFirstArg(postArgs);
        Log.d(TAG, "selection is: " + selection);

        //send user update
        String displayString = "Selected: " + selection;
        sendMessage(vcServer, displayString);

        //send new selection to the system, whoever sent out a request for a selection will receive this response
        JSONObject data = new JSONObject();
        try{
            data.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.ACTION_SELECT_COMMAND);
            data.put(MessageTypes.SELECTION, selection);
            vcServer.dataObservable.onNext(data);
        } catch (JSONException e){
            e.printStackTrace();
        }

        return true;
    }

}

