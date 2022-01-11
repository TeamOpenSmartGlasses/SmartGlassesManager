package com.google.mediapipe.apps.wearableai.database.person;

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

public class PersonCreator {
    public static final String TAG = "WearableAi_PersonCreator";

    public static long create(Long personId, String argKey, String argValue, long timestamp, long mediaId, PersonRepository repo) {
        /*
        Location may return right away or in a minute, null or not
        Because of this, insert each voicecommand without location synchronously, and after getting id back
            get location and update the voicecommand with location results whenever they arrive.
         */

        PersonEntity person = new PersonEntity(personId, argKey, argValue, timestamp, mediaId);
        long id = repo.insert(person);  // This insert blocks until database write has completed
        Log.d(TAG, "Saved person info database");
        return id;
    }
}
