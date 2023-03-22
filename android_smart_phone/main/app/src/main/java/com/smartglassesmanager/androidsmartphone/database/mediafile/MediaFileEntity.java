package com.smartglassesmanager.androidsmartphone.database.mediafile;

//originally from MXT: Memory Expansion Tools
//Jeremy Stairs (stairs1) and Cayden Pierce
//https://github.com/stairs1/memory-expansion-tools

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "MediaFileTable")
public class MediaFileEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int id = 0;

    @NonNull
    @ColumnInfo(name = "mediaType")
    private String mediaType;

    @NonNull
    @ColumnInfo(name = "localPath")
    private String localPath;

    @NonNull
    @ColumnInfo(name = "startTimestamp")
    private long startTimestamp;

    @ColumnInfo(name = "endTimestamp")
    private long endTimestamp;

    public MediaFileEntity(@NonNull String localPath, @NonNull String mediaType, @NonNull long startTimestamp, long endTimestamp) {
        this.localPath = localPath;
        this.mediaType = mediaType;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
    }

    public String getLocalPath(){return this.localPath;}
    public String getMediaType(){return this.mediaType;}
    public long getStartTimestamp(){return this.startTimestamp;}
    public long getEndTimestamp(){return this.endTimestamp;}
    public int getId(){return this.id;}
    public void setId(int id){this.id = id;}
}
