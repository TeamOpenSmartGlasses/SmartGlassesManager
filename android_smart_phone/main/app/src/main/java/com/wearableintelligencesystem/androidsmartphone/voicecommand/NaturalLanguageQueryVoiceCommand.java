package com.wearableintelligencesystem.androidsmartphone.voicecommand;

import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;

import com.wearableintelligencesystem.androidsmartphone.database.voicecommand.VoiceCommandRepository;
import com.wearableintelligencesystem.androidsmartphone.database.voicecommand.VoiceCommandDao;
import com.wearableintelligencesystem.androidsmartphone.comms.MessageTypes;
import com.wearableintelligencesystem.androidsmartphone.database.voicecommand.VoiceCommandCreator;
import com.wearableintelligencesystem.androidsmartphone.database.voicecommand.VoiceCommandEntity;


class NaturalLanguageQueryVoiceCommand extends VoiceCommand {
    private String TAG = "WearableAi_NaturalLanguageQueryVoiceCommand";

    NaturalLanguageQueryVoiceCommand(Context context){
        super(context);
        this.commandName = "natural language query";
        this.commandList = new ArrayList<>(Arrays.asList(new String [] {"question"}));
        this.wakeWordList = new ArrayList<>(Arrays.asList(new String [] {}));
    }

    @Override
    public boolean runCommand(VoiceCommandServer vcServer, String preArgs, String wakeWord, int command, String postArgs, long commandTime, long transcriptId){
        Log.d(TAG, "Running command: " + this.commandList.get(command)); 

        //parse command
        String commandSpoken = this.commandList.get(command);

        //save master command
        VoiceCommandCreator.create(this.commandName, commandSpoken, wakeWord, true, null, null, commandTime, preArgs, postArgs,"asg_transcript", transcriptId, vcServer.mVoiceCommandRepository);

        //send user update
        String displayString = "Querying: " + postArgs;
        sendResult(vcServer, true, this.commandName, displayString);
        //sendMessage(vcServer, displayString);

        //ask system to do natural language query and send to ASG on result
        JSONObject data = new JSONObject();
        try{
            data.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.NATURAL_LANGUAGE_QUERY);
            data.put(MessageTypes.TEXT_QUERY, postArgs);
            vcServer.dataObservable.onNext(data);
        } catch (JSONException e){
            e.printStackTrace();
        }

        return true;
        }

}
