package com.google.mediapipe.apps.wearableai.voicecommand;

import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;

import com.google.mediapipe.apps.wearableai.database.voicecommand.VoiceCommandRepository;
import com.google.mediapipe.apps.wearableai.database.voicecommand.VoiceCommandDao;
import com.google.mediapipe.apps.wearableai.comms.MessageTypes;
import com.google.mediapipe.apps.wearableai.database.voicecommand.VoiceCommandCreator;
import com.google.mediapipe.apps.wearableai.database.voicecommand.VoiceCommandEntity;


class SearchEngineVoiceCommand extends VoiceCommand {
    private String TAG = "WearableAi_SearchEngineVoiceCommand";

    SearchEngineVoiceCommand(Context context){
        super(context);
        this.commandName = "search engine";
        this.commandList = new ArrayList<>(Arrays.asList(new String [] {"wikipedia", "search engine", "google", "search for", "search web for", "show web results for"}));
        this.wakeWordList = new ArrayList<>(Arrays.asList(new String [] {"search web for", "show web results for"}));
    }

    @Override
    public boolean runCommand(VoiceCommandServer vcServer, String preArgs, String wakeWord, int command, String postArgs, long commandTime, long transcriptId){
        Log.d(TAG, "Running command: " + this.commandList.get(command)); 

        //parse command
        String commandSpoken = this.commandList.get(command);

        //save master command
        VoiceCommandCreator.create(this.commandName, commandSpoken, wakeWord, true, null, null, commandTime, "asg_transcript", transcriptId, vcServer.mVoiceCommandRepository);

        //send user update
        String displayString = "Searching for: " + postArgs;
        //sendResult(vcServer, true, this.commandName, displayString);
        sendMessage(vcServer, displayString);

        //ask system to do natural language query and send to ASG on result
        JSONObject data = new JSONObject();
        try{
            data.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.SEARCH_ENGINE_RESULT);
            data.put(MessageTypes.TEXT_QUERY, postArgs);
            vcServer.dataObservable.onNext(data);
        } catch (JSONException e){
            e.printStackTrace();
        }

        return true;
        }

}
