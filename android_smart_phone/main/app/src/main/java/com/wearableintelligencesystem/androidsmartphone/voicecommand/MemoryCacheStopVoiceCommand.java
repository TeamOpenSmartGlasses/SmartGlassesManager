package com.wearableintelligencesystem.androidsmartphone.voicecommand;

import android.content.Context;
import android.util.Log;

import com.wearableintelligencesystem.androidsmartphone.database.memorycache.MemoryCacheTimesCreator;
import com.wearableintelligencesystem.androidsmartphone.database.voicecommand.VoiceCommandCreator;

import java.util.ArrayList;
import java.util.Arrays;

class MemoryCacheStopVoiceCommand extends VoiceCommand {
    private String TAG = "WearableAi_MemoryCacheStopVoiceCommand";

    MemoryCacheStopVoiceCommand(Context context){
        super(context);
        this.commandName = "finish memory cache";
        this.commandList = new ArrayList<>(Arrays.asList(new String [] {"finish memory cache"}));
        this.wakeWordList = new ArrayList<>(Arrays.asList(new String [] {}));
        this.noArgs = true;
    }

    @Override
    public boolean runCommand(VoiceCommandServer vcServer, String preArgs, String wakeWord, int command, String postArgs, long commandTime, long transcriptId){
        Log.d(TAG, "Running command: " + this.commandList.get(command));

        //parse command
        String commandSpoken = this.commandList.get(command);

        //save master command
        VoiceCommandCreator.create(this.commandName, commandSpoken, wakeWord, true, null, null, commandTime, preArgs, postArgs,"asg_transcript", transcriptId, vcServer.mVoiceCommandRepository);

        //stop the memory cache
        //find the active cache
        Long activeCacheId = vcServer.mMemoryCacheRepository.getActiveCache();

        //make cache active start now
        String displayString;
        boolean success;
        if (activeCacheId != null) {
            vcServer.mMemoryCacheRepository.updateCacheStopTime(activeCacheId, commandTime);
            displayString = "Stopped memory cache.";
            success = true;
        } else {
            displayString = "No active memory cache to stop.";
            success = false;
        }

        //tel user what we did
        sendResult(vcServer, success, this.commandName, displayString);
        return true;
    }
}


