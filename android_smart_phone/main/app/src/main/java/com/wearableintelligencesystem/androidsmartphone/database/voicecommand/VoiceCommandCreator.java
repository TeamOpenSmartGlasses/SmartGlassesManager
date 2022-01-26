package com.wearableintelligencesystem.androidsmartphone.database.voicecommand;

//originally from MXT: Memory Expansion Tools
//Jeremy Stairs (stairs1) and Cayden Pierce
//https://github.com/stairs1/memory-expansion-tools

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.util.Log;

public class VoiceCommandCreator {
    public static final String TAG = "WearableAi_VoiceCommandCreator";

    public static void create(String commandName, String commandSpoken, String wakeWord, boolean isMaster, String argKey, String argValue, long timestamp, String preArgs, String postArgs, String medium, long transcriptId, VoiceCommandRepository repo) {
        /*
        Location may return right away or in a minute, null or not
        Because of this, insert each voicecommand without location synchronously, and after getting id back
            get location and update the voicecommand with location results whenever they arrive.
         */

        VoiceCommandEntity voiceCommand = new VoiceCommandEntity(commandName, commandSpoken, wakeWord, isMaster, argKey, argValue, timestamp, preArgs, postArgs, medium, transcriptId);
        long id = repo.insert(voiceCommand);  // This insert blocks until database write has completed
        Log.d(TAG, "Saved voice command to database");
    }
}
