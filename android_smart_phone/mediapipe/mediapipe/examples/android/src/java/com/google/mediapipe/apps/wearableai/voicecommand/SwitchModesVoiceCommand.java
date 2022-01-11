package com.google.mediapipe.apps.wearableai.voicecommand;

import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;

import android.util.Pair;

import com.google.mediapipe.apps.wearableai.database.voicecommand.VoiceCommandRepository;
import com.google.mediapipe.apps.wearableai.database.voicecommand.VoiceCommandDao;
import com.google.mediapipe.apps.wearableai.comms.MessageTypes;
import com.google.mediapipe.apps.wearableai.database.voicecommand.VoiceCommandCreator;
import com.google.mediapipe.apps.wearableai.database.voicecommand.VoiceCommandEntity;


class SwitchModesVoiceCommand extends VoiceCommand {
    private String TAG = "WearableAi_SwitchModesVoiceCommand";

    protected ArrayList<Pair<String, String>> modesList;

    SwitchModesVoiceCommand(Context context){
        super(context);
        this.commandName = "switch modes";
        this.commandList = new ArrayList<>(Arrays.asList(new String [] {"switch modes", "switch to mode"}));
        this.wakeWordList = new ArrayList<>(Arrays.asList(new String [] {}));

//        this.modesList = new ArrayList<>(Arrays.asList(new Pair<String, String> [] {
//                        new Pair("live life captions", MessageTypes.MODE_VISUAL_SEARCH),
//                        new Pair("social mode", MessageTypes.MODE_VISUAL_SEARCH),
//                        new Pair("visual search", MessageTypes.MODE_VISUAL_SEARCH)
//                    }
//                  )
//                );

        modesList = new ArrayList<>();
        modesList.add(Pair.create("live life captions", MessageTypes.MODE_LIVE_LIFE_CAPTIONS));
        modesList.add(Pair.create("social mode", MessageTypes.MODE_SOCIAL_MODE));
        modesList.add(Pair.create("visual search", MessageTypes.MODE_VISUAL_SEARCH));
    }

    @Override
    public boolean runCommand(VoiceCommandServer vcServer, String preArgs, String wakeWord, int command, String postArgs, long commandTime, long transcriptId){
        Log.d(TAG, "Running command: " + this.commandList.get(command)); 

        //parse command
        String commandSpoken = this.commandList.get(command);

        //save master command
        VoiceCommandCreator.create(this.commandName, commandSpoken, wakeWord, true, null, null, commandTime, "asg_transcript", transcriptId, vcServer.mVoiceCommandRepository);

        //parse the argument to find which mode they want to switch to
        int modeMatchIdx = -1;
        String naturalLanguageMode = "";
        String newMode = "";
        Log.d(TAG, "Finding near matches");
        for (int i = 0; i < modesList.size(); i++){
            naturalLanguageMode = modesList.get(i).first;
            Log.d(TAG, "args: " + postArgs + "; mode: " + modesList.get(i).first);
            int modeMatchChar = nlpUtils.findNearMatches(postArgs, naturalLanguageMode, 0.8);
            if (modeMatchChar != -1){
                modeMatchIdx = i;
                newMode = modesList.get(i).second;
                Log.d(TAG, "Found match: " + newMode);
                break;
            }
        }

        if (modeMatchIdx == -1){
            //send user update
            String displayString = "No valid mode detected.";
            sendMessage(vcServer, displayString);
            return false;
        }

        //send user update
        String displayString = "Switching to mode: " + naturalLanguageMode;
        sendMessage(vcServer, displayString);

        //ask system to send ASG switch mode command
        JSONObject data = new JSONObject();
        try{
            data.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.ACTION_SWITCH_MODES);
            data.put(MessageTypes.NEW_MODE, newMode);
            vcServer.dataObservable.onNext(data);
        } catch (JSONException e){
            e.printStackTrace();
        }

        return true;
        }

}
