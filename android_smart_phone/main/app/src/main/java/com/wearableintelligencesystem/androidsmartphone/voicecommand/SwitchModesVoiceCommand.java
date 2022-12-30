package com.wearableintelligencesystem.androidsmartphone.voicecommand;

import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;

import android.util.Pair;

import com.wearableintelligencesystem.androidsmartphone.database.voicecommand.VoiceCommandRepository;
import com.wearableintelligencesystem.androidsmartphone.database.voicecommand.VoiceCommandDao;
import com.wearableintelligencesystem.androidsmartphone.comms.MessageTypes;
import com.wearableintelligencesystem.androidsmartphone.database.voicecommand.VoiceCommandCreator;
import com.wearableintelligencesystem.androidsmartphone.database.voicecommand.VoiceCommandEntity;
import com.wearableintelligencesystem.androidsmartphone.nlp.FuzzyMatch;

class SwitchModesVoiceCommand extends VoiceCommand {
    private String TAG = "WearableAi_SwitchModesVoiceCommand";

    protected ArrayList<Pair<String, String>> modesList;

    SwitchModesVoiceCommand(Context context){
        super(context);
        this.commandName = "switch modes";
        this.commandList = new ArrayList<>(Arrays.asList(new String [] {"run"}));
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
        modesList.add(Pair.create("conversation", MessageTypes.MODE_CONVERSATION_MODE));
        modesList.add(Pair.create("object translate", MessageTypes.MODE_OBJECT_TRANSLATE));
        modesList.add(Pair.create("blank", MessageTypes.MODE_BLANK));
        modesList.add(Pair.create("speech translate", MessageTypes.MODE_LANGUAGE_TRANSLATE));
        modesList.add(Pair.create("contextual search", MessageTypes.MODE_CONTEXTUAL_SEARCH));

    }

    @Override
    public boolean runCommand(VoiceCommandServer vcServer, String preArgs, String wakeWord, int command, String postArgs, long commandTime, long transcriptId){
        Log.d(TAG, "Running command: " + this.commandList.get(command)); 

        //parse command
        String commandSpoken = this.commandList.get(command);

        //save master command
        VoiceCommandCreator.create(this.commandName, commandSpoken, wakeWord, true, null, null, commandTime, preArgs, postArgs,"asg_transcript", transcriptId, vcServer.mVoiceCommandRepository);

        //parse the argument to find which mode they want to switch to
        int modeMatchIdx = -1;
        String naturalLanguageMode = "";
        String newMode = "";
        Log.d(TAG, "Finding near matches");
        for (int i = 0; i < modesList.size(); i++){
            naturalLanguageMode = modesList.get(i).first;
            Log.d(TAG, "args: " + postArgs + "; mode: " + modesList.get(i).first);
            FuzzyMatch modeMatchChar = nlpUtils.findNearMatches(postArgs, naturalLanguageMode, 0.85);
            if (modeMatchChar != null && modeMatchChar.getIndex() != -1){
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

        //special actions ASP takes based on the mode we are entering
        try {
            JSONObject modeMessage = new JSONObject();
            if (newMode.equals(MessageTypes.MODE_LANGUAGE_TRANSLATE)){
                //get the language we want to translate from into base language
                String naturalLanguageLanguage = this.getFirstArg(postArgs.trim().substring(naturalLanguageMode.length()));
                if (naturalLanguageLanguage.equals("") || naturalLanguageLanguage == null){
                    String displayString = "Please provide a target language";
                    sendMessage(vcServer, displayString);
                    return false;
                }
                Log.d(TAG, "____________*****************______________");
                Log.d(TAG, postArgs);
                Log.d(TAG, naturalLanguageMode);
                Log.d(TAG, naturalLanguageLanguage);
                modeMessage.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.START_FOREIGN_LANGUAGE_ASR);
                modeMessage.put(MessageTypes.START_FOREIGN_LANGUAGE_SOURCE_LANGUAGE_NAME, naturalLanguageLanguage);
                vcServer.dataObservable.onNext(modeMessage);
            } else if (newMode.equals(MessageTypes.MODE_OBJECT_TRANSLATE)) {
                JSONObject objectMessage = new JSONObject();
                objectMessage.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.START_OBJECT_DETECTION);
                vcServer.dataObservable.onNext(objectMessage);
            } else if (newMode.equals(MessageTypes.MODE_CONTEXTUAL_SEARCH)) {
                JSONObject objectMessage = new JSONObject();
                objectMessage.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.START_CONTEXTUAL_SEARCH);
                vcServer.dataObservable.onNext(objectMessage);
            }else {
                modeMessage.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.STOP_FOREIGN_LANGUAGE_ASR);
                modeMessage.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.STOP_OBJECT_DETECTION);
                modeMessage.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.STOP_CONTEXTUAL_SEARCH);
                vcServer.dataObservable.onNext(modeMessage);
            }
        } catch (JSONException e){
            e.printStackTrace();
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
