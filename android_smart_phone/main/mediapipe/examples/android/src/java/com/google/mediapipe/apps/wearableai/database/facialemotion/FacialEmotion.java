package com.google.mediapipe.apps.wearableai.database.facialemotion;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "FacialEmotionTable")
public class FacialEmotion {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int id = 0;

    @NonNull
    @ColumnInfo(name = "facial_emotion")
    private String facialEmotion;

    @NonNull
    @ColumnInfo(name = "timestamp")
    private long timestamp;

    @NonNull
    @ColumnInfo(name = "medium")
    private String medium;

    @ColumnInfo(name = "location")
    private Location location;

    @ColumnInfo(name = "address")
    private String address;

    public FacialEmotion(@NonNull String facialEmotion, @NonNull long timestamp, @NonNull String medium, Location location, String address) {
        this.facialEmotion = facialEmotion;
        this.timestamp = timestamp;
        this.medium = medium;
        this.location = location;
        this.address = address;
    }

    @Ignore
     public FacialEmotion(@NonNull String facialEmotion, @NonNull long timestamp, @NonNull String medium){
        this.facialEmotion = facialEmotion;
        this.timestamp = timestamp;
        this.medium = medium;
     }

    public String getFacialEmotion(){return this.facialEmotion;}
    public long getTimestamp(){return this.timestamp;}
    public String getMedium(){return this.medium;}
    public Location getLocation(){return this.location;}
    public void setLocation(Location location){this.location = location;}
    public String getAddress(){return this.address;}
    public void setAddress(String address){this.address = address;}
    public int getId(){return this.id;}
    public void setId(int id){this.id = id;}
}
