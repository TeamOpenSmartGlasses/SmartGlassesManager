package com.google.mediapipe.apps.wearableai.database.phrase;

//originally from MXT: Memory Expansion Tools
//Jeremy Stairs (stairs1) and Cayden Pierce
//https://github.com/stairs1/memory-expansion-tools

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "PhraseTable")
public class Phrase {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int id = 0;

    @NonNull
    @ColumnInfo(name = "phrase")
    private String phrase;

    @NonNull
    @ColumnInfo(name = "timestamp")
    private Date timestamp;

    @NonNull
    @ColumnInfo(name = "medium")
    private String medium;

    @ColumnInfo(name = "location")
    private Location location;

    @ColumnInfo(name = "address")
    private String address;

    public Phrase(@NonNull String phrase, @NonNull Date timestamp, @NonNull String medium, Location location, String address) {
        this.phrase = phrase;
        this.timestamp = timestamp;
        this.medium = medium;
        this.location = location;
        this.address = address;
    }

    @Ignore
     public Phrase(@NonNull String phrase, @NonNull Date timestamp, @NonNull String medium){
        this.phrase = phrase;
        this.timestamp = timestamp;
        this.medium = medium;
     }

    public String getPhrase(){return this.phrase;}
    public Date getTimestamp(){return this.timestamp;}
    public String getMedium(){return this.medium;}
    public Location getLocation(){return this.location;}
    public void setLocation(Location location){this.location = location;}
    public String getAddress(){return this.address;}
    public void setAddress(String address){this.address = address;}
    public int getId(){return this.id;}
    public void setId(int id){this.id = id;}
}
