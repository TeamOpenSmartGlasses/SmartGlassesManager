package com.smartglassesmanager.androidsmartphone.database.memorycache;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "MemoryCacheTable")
public class MemoryCache {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int id = 0;

    @ColumnInfo(name = "cache_name")
    private String cacheName;

    @ColumnInfo(name = "stopTimestamp")
    private Long stopTimestamp;

    @NonNull
    @ColumnInfo(name = "startTimestamp")
    private long startTimestamp;

    public MemoryCache(@NonNull long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    @Ignore
    public MemoryCache(@NonNull long startTimestamp, Long stopTimestamp) {
        this.startTimestamp = startTimestamp;
        this.stopTimestamp = stopTimestamp;
    }

    public long getStartTimestamp(){return this.startTimestamp;}
    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }
    public String getCacheName(){return this.cacheName;}
    public void setCacheName(String cacheName){this.cacheName = cacheName;}
    public int getId(){return this.id;}
    public void setId(int id){this.id = id;}


    public Long getStopTimestamp() {
        return stopTimestamp;
    }

    public void setStopTimestamp(Long stopTimestamp) {
        this.stopTimestamp = stopTimestamp;
    }
}
