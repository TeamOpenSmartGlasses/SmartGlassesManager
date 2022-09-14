package com.wearableintelligencesystem.androidsmartphone.voicecommand;

import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;

import com.wearableintelligencesystem.androidsmartphone.database.voicecommand.VoiceCommandRepository;
import com.wearableintelligencesystem.androidsmartphone.database.voicecommand.VoiceCommandDao;
import com.wearableintelligencesystem.androidsmartphone.database.voicecommand.VoiceCommandCreator;
import com.wearableintelligencesystem.androidsmartphone.database.voicecommand.VoiceCommandEntity;

class VoiceNoteVoiceCommand extends VoiceCommand {
    private String TAG = "WearableAi_MxtVoiceCommand";

    VoiceNoteVoiceCommand(Context context){
        super(context);
        this.commandName = "save speech";
        this.commandList = new ArrayList<>(Arrays.asList(new String [] {"save speech"}));
        this.wakeWordList = new ArrayList<>(Arrays.asList(new String [] {}));
    }

    @Override
    public boolean runCommand(VoiceCommandServer vcServer, String preArgs, String wakeWord, int command, String postArgs, long commandTime, long transcriptId){
        Log.d(TAG, "Running command: " + this.commandList.get(command)); 

        //parse command
        String commandSpoken = this.commandList.get(command);

        //save master command
        VoiceCommandCreator.create(this.commandName, commandSpoken, wakeWord, true, null, null, commandTime, preArgs, postArgs, "asg_transcript", transcriptId, vcServer.mVoiceCommandRepository);

        //save raw data - eventually this should be its own table
//        String spokenText = preArgs + " " + wakeWord + " " + commandSpoken + " " + postArgs;
//        Log.d(TAG, "Saving voice command with spoken text == " + spokenText);
//        VoiceCommandCreator.create(this.commandName, commandSpoken, wakeWord, false, "spoken_text", spokenText, commandTime, "asg_transcript", transcriptId, vcServer.mVoiceCommandRepository);

        //find tags and return a list of the tags that were found
        ArrayList<String> foundTags = this.parseKeyValueArgs(postArgs, "tag");
        String displayString = "Saved note: " + postArgs + "\n";
        if (foundTags != null && foundTags.size() != 0){ //save the tags and add them to the display string for user
            //make the display string
            displayString = displayString + "\nSaved in tag bins: ";
            for (int i = 0; i < foundTags.size(); i++){
                //save tag command
                Log.d(TAG, "Saved tag: " + foundTags.get(i));
                VoiceCommandCreator.create(this.commandName, commandSpoken, wakeWord, false, "tag", foundTags.get(i), commandTime, preArgs, postArgs,"asg_transcript", transcriptId, vcServer.mVoiceCommandRepository);

                //add to display string
                displayString = displayString + foundTags.get(i) + ", ";
            }
            displayString = displayString.substring(0, displayString.length() - 2); //get rid of last comma and space
        }

        sendResult(vcServer, true, this.commandName, displayString);
        return true;
    }


}
