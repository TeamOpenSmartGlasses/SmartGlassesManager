package com.smartglassesmanager.androidsmartphone.database.memorycache;

//originally from MXT: Memory Expansion Tools
//Jeremy Stairs (stairs1) and Cayden Pierce
//https://github.com/stairs1/memory-expansion-tools

import androidx.annotation.NonNull;

public class MemoryCacheCreator {
    public static final String TAG = "WearableAi_MemoryCacheCreator";

    public static long create(@NonNull long startTimestamp, MemoryCacheRepository repo){
        MemoryCache cache = new MemoryCache(startTimestamp);
        long id = repo.insert(cache);  // This insert blocks until database write has completed

        return id;
    }
}
