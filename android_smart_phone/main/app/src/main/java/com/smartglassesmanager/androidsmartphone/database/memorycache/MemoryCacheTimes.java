package com.smartglassesmanager.androidsmartphone.database.memorycache;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "MemoryCacheTimesTable")
public class MemoryCacheTimes {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int id = 0;


    @NonNull
    @ColumnInfo(name = "cache_id")
    private long cacheId;

    @NonNull
    @ColumnInfo(name = "timestamp")
    private long timestamp;

    //tells us if we are starting (true) or stopping (false) the cache from being active
    @NonNull
    @ColumnInfo(name = "start")
    private boolean start;

    public MemoryCacheTimes(@NonNull long cacheId, @NonNull long timestamp, @NonNull boolean start) {
        this.cacheId = cacheId;
        this.timestamp = timestamp;
        this.start = start;
    }

    public long getTimestamp(){return this.timestamp;}
    public boolean getStart(){return this.start;}
    public int getId(){return this.id;}
    public void setId(int id){this.id = id;}

    public long getCacheId() {
        return cacheId;
    }

    public void setCacheId(long cacheId) {
        this.cacheId = cacheId;
    }
}
