package com.google.mediapipe.apps.wearableai.voicecommand;

import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;

import com.google.mediapipe.apps.wearableai.database.voicecommand.VoiceCommandRepository;
import com.google.mediapipe.apps.wearableai.database.voicecommand.VoiceCommandDao;
import com.google.mediapipe.apps.wearableai.database.voicecommand.VoiceCommandCreator;
import com.google.mediapipe.apps.wearableai.database.voicecommand.VoiceCommandEntity;

class MxtVoiceCommand extends VoiceCommand {
    private String TAG = "WearableAi_MxtVoiceCommand";

    MxtVoiceCommand(){
        this.commandName = "mxt";
        this.commandList = new ArrayList<>(Arrays.asList(new String [] {"mxt", "m x t", "mx t", "save speech", "remember speech"}));
        this.wakeWordList = new ArrayList<>(Arrays.asList(new String [] {"mxt", "m x t", "mx t"}));
    }

    @Override
    public boolean runCommand(VoiceCommandServer vcServer, String preArgs, String wakeWord, int command, String postArgs, long commandTime){
        Log.d(TAG, "Running command: " + this.commandList.get(command)); 

        //parse command
        String commandSpoken = this.commandList.get(command);

        //save master command
        VoiceCommandCreator.create(this.commandName, commandSpoken, wakeWord, true, null, null, commandTime, "asg_transcript", vcServer.mVoiceCommandRepository);

        //find tags and return a list of the tags that were found
        ArrayList<String> foundTags = this.parseKeyValueArgs(postArgs, "tag");
        for (int i = 0; i < foundTags.size(); i++){
            //save tag command
            Log.d(TAG, "Saving tag: " + foundTags.get(i));
            VoiceCommandCreator.create(this.commandName, commandSpoken, wakeWord, false, "tag", foundTags.get(i), commandTime, "asg_transcript", vcServer.mVoiceCommandRepository);
        }

        return true;
    }


}
