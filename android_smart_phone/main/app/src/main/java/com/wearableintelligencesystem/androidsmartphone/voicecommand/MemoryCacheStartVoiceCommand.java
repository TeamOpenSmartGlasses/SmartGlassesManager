package com.wearableintelligencesystem.androidsmartphone.voicecommand;

import android.content.Context;
import android.util.Log;

import com.wearableintelligencesystem.androidsmartphone.database.memorycache.MemoryCacheCreator;
import com.wearableintelligencesystem.androidsmartphone.database.memorycache.MemoryCacheTimesCreator;
import com.wearableintelligencesystem.androidsmartphone.database.voicecommand.VoiceCommandCreator;

import java.util.ArrayList;
import java.util.Arrays;

class MemoryCacheStartVoiceCommand extends VoiceCommand {
    private String TAG = "WearableAi_MemoryCacheStartVoiceCommand";

    MemoryCacheStartVoiceCommand(Context context){
        super(context);
        this.commandName = "start memory cache";
        this.commandList = new ArrayList<>(Arrays.asList(new String [] {"start memory cache"}));
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

        //find the active cache
        Long activeCacheId = vcServer.mMemoryCacheRepository.getActiveCache();

        String displayString;
        boolean success;
        if (activeCacheId != null){
            displayString = "Memory cache already running. Finish current active cache to start a new one.";
            success = false;
        } else {
            //start memory cache by creating cache and creating start memory cache event
            //make new cache
            long cacheId = MemoryCacheCreator.create(commandTime, vcServer.mMemoryCacheRepository);

            displayString = "Started new memory cache.";
            success = true;
        }

        sendResult(vcServer, success, this.commandName, displayString);
        return true;
    }


}

