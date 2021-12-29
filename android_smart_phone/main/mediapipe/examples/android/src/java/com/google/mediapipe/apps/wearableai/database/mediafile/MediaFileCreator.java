package com.google.mediapipe.apps.wearableai.database.mediafile;

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
import java.util.List;
import android.util.Log;

public class MediaFileCreator {
    public static final String TAG = "WearableAi_MediaFileCreator";

    public static long create(String localPath, String mediaType, long startTimestamp, long endTimestamp, MediaFileRepository repo) {

        MediaFileEntity mediaFile = new MediaFileEntity(localPath, mediaType, startTimestamp, endTimestamp);
        long id = repo.insert(mediaFile);  // This insert blocks until database write has completed
        Log.d(TAG, "Saved mediaFile to database");
        return id;
    }
}
