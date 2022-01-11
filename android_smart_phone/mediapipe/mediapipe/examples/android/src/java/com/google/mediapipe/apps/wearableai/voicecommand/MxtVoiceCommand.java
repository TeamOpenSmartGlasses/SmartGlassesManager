package com.google.mediapipe.apps.wearableai.voicecommand;

import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;

import com.google.mediapipe.apps.wearableai.database.voicecommand.VoiceCommandRepository;
import com.google.mediapipe.apps.wearableai.database.voicecommand.VoiceCommandDao;
import com.google.mediapipe.apps.wearableai.database.voicecommand.VoiceCommandCreator;
import com.google.mediapipe.apps.wearableai.database.voicecommand.VoiceCommandEntity;

class MxtVoiceCommand extends VoiceCommand {
    private String TAG = "WearableAi_MxtVoiceCommand";

    MxtVoiceCommand(Context context){
        super(context);
        this.commandName = "save speech";
        this.commandList = new ArrayList<>(Arrays.asList(new String [] {"save speech", "remember speech"}));
        this.wakeWordList = new ArrayList<>(Arrays.asList(new String [] {"save speech", "remember speech"}));
    }

    @Override
    public boolean runCommand(VoiceCommandServer vcServer, String preArgs, String wakeWord, int command, String postArgs, long commandTime, long transcriptId){
        Log.d(TAG, "Running command: " + this.commandList.get(command)); 

        //parse command
        String commandSpoken = this.commandList.get(command);

        //save master command
        VoiceCommandCreator.create(this.commandName, commandSpoken, wakeWord, true, null, null, commandTime, "asg_transcript", transcriptId, vcServer.mVoiceCommandRepository);

        //find tags and return a list of the tags that were found
        ArrayList<String> foundTags = this.parseKeyValueArgs(postArgs, "tag");
        String displayString = null;
        if (foundTags != null && foundTags.size() != 0){ //save the tags and add them to the display string for user
            //make the display string
            displayString = "Saving in tag bins: ";
            for (int i = 0; i < foundTags.size(); i++){
                //save tag command
                Log.d(TAG, "Saving tag: " + foundTags.get(i));
                VoiceCommandCreator.create(this.commandName, commandSpoken, wakeWord, false, "tag", foundTags.get(i), commandTime, "asg_transcript", transcriptId, vcServer.mVoiceCommandRepository);

                //add to display string
                displayString = displayString + foundTags.get(i) + ", ";
            }
            displayString = displayString.substring(0, displayString.length() - 2); //get rid of last comma and space
        }

        sendResult(vcServer, true, this.commandName, displayString);
        return true;
    }


}
