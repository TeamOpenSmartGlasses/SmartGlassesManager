package com.wearableintelligencesystem.androidsmartphone.voicecommand;

import android.content.Context;
import android.util.Log;

import com.wearableintelligencesystem.androidsmartphone.comms.MessageTypes;
import com.wearableintelligencesystem.androidsmartphone.database.voicecommand.VoiceCommandCreator;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;


class ReferenceTranslateVoiceCommand extends VoiceCommand {
    private String TAG = "WearableAi_RefrenceTranslateVoiceCommand";

    ReferenceTranslateVoiceCommand(Context context){
        super(context);
        this.commandName = "translate word";
        this.commandList = new ArrayList<>(Arrays.asList(new String [] {"translate"}));
        this.wakeWordList = new ArrayList<>(Arrays.asList(new String [] {}));
    }

    @Override
    public boolean runCommand(VoiceCommandServer vcServer, String preArgs, String wakeWord, int command, String postArgs, long commandTime, long transcriptId){
        Log.d(TAG, "Running command: " + this.commandList.get(command)); 

        //parse command
        String commandSpoken = this.commandList.get(command);

        //save master command
        VoiceCommandCreator.create(this.commandName, commandSpoken, wakeWord, true, null, null, commandTime, preArgs, postArgs, "asg_transcript", transcriptId, vcServer.mVoiceCommandRepository);

        //send user update
        String displayString = "Translating: " + postArgs;

        //sendResult(vcServer, true, this.commandName, displayString);
        sendMessage(vcServer, displayString);

        //ask system to do natural language query and send to ASG on result
        JSONObject data = new JSONObject();
        try{
            data.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.REFERENCE_TRANSLATE_SEARCH_QUERY);
            data.put(MessageTypes.REFERENCE_TRANSLATE_DATA, postArgs);
            vcServer.dataObservable.onNext(data);
        } catch (JSONException e){
            e.printStackTrace();
        }

        return true;
        }

}
