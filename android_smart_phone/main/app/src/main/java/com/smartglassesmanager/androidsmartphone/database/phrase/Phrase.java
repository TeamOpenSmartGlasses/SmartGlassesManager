package com.smartglassesmanager.androidsmartphone.database.phrase;

//originally from MXT: Memory Expansion Tools
//Jeremy Stairs (stairs1) and Cayden Pierce
//https://github.com/stairs1/memory-expansion-tools

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import java.io.Serializable;

//this is serializable so we can pass it through a bundle or turn it into json. In the future, making is parecebable may make sense for program speed. Right now, serializable makes sense as it's faster to implement and we are nowhere near performance issues for the one or two phrases we must pass around
@Entity(tableName = "PhraseTable")
public class Phrase implements Serializable {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id = 0;

    @NonNull
    @ColumnInfo(name = "phrase")
    private String phrase;

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

    public Phrase(@NonNull String phrase, @NonNull long timestamp, @NonNull String medium, Location location, String address) {
        super(); //serializable
        this.phrase = phrase;
        this.timestamp = timestamp;
        this.medium = medium;
        this.location = location;
        this.address = address;
    }

    @Ignore
     public Phrase(@NonNull String phrase, @NonNull long timestamp, @NonNull String medium){
        this.phrase = phrase;
        this.timestamp = timestamp;
        this.medium = medium;
     }

    public String getPhrase(){return this.phrase;}
    public long getTimestamp(){return this.timestamp;}
    public String getMedium(){return this.medium;}
    public Location getLocation(){return this.location;}
    public void setLocation(Location location){this.location = location;}
    public String getAddress(){return this.address;}
    public void setAddress(String address){this.address = address;}
    public long getId(){return this.id;}
    public void setId(long id){this.id = id;}
}
