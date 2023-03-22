package com.smartglassesmanager.androidsmartphone.database.mediafile;

//originally from MXT: Memory Expansion Tools
//Jeremy Stairs (stairs1) and Cayden Pierce
//https://github.com/stairs1/memory-expansion-tools

public class MediaFileCreator {
    public static final String TAG = "WearableAi_MediaFileCreator";

    public static long create(String localPath, String mediaType, long startTimestamp, long endTimestamp, MediaFileRepository repo) {

        MediaFileEntity mediaFile = new MediaFileEntity(localPath, mediaType, startTimestamp, endTimestamp);
        long id = repo.insert(mediaFile);  // This insert blocks until database write has completed
        return id;
    }
}
