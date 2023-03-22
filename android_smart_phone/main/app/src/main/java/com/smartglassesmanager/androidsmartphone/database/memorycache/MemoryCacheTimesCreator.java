package com.smartglassesmanager.androidsmartphone.database.memorycache;

//originally from MXT: Memory Expansion Tools
//Jeremy Stairs (stairs1) and Cayden Pierce
//https://github.com/stairs1/memory-expansion-tools

import androidx.annotation.NonNull;

public class MemoryCacheTimesCreator {
    public static final String TAG = "WearableAi_MemoryCacheTimesCreator";

    public static long create(@NonNull long cacheId, @NonNull long timestamp, @NonNull boolean start, MemoryCacheRepository repo){
        MemoryCacheTimes cacheTime = new MemoryCacheTimes(cacheId, timestamp, start);
        long id = repo.addCacheTime(cacheTime);  // This insert blocks until database write has completed

        return id;
    }
}
