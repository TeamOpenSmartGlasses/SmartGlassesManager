package com.google.mediapipe.apps.wearableai.database.voicecommand;

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

//each voice command should be tied to a specific Phrase/Transcript id eventually, then we don't have to repeat information here and all data is interconnected
@Entity(tableName = "VoiceCommandTable")
public class VoiceCommandEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int id = 0;

    @NonNull
    @ColumnInfo(name = "preArgs")
    private String preArgs;

    @NonNull
    @ColumnInfo(name = "wakeWord")
    private String wakeWord;

    @NonNull
    @ColumnInfo(name = "command")
    private String command;

    @NonNull
    @ColumnInfo(name = "postArgs")
    private String postArgs;

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

    public VoiceCommandEntity(@NonNull String preArgs, @NonNull String wakeWord, @NonNull String command, @NonNull String postArgs, @NonNull Date timestamp, @NonNull String medium, Location location, String address) {
        this.preArgs = preArgs;
        this.wakeWord = wakeWord;
        this.command = command;
        this.postArgs = postArgs;
        this.timestamp = timestamp;
        this.medium = medium;
        this.location = location;
        this.address = address;
    }

    @Ignore
    public VoiceCommandEntity(@NonNull String preArgs, @NonNull String wakeWord, @NonNull String command, @NonNull String postArgs, @NonNull Date timestamp, @NonNull String medium) {
        this.preArgs = preArgs;
        this.wakeWord = wakeWord;
        this.command = command;
        this.postArgs = postArgs;
        this.timestamp = timestamp;
        this.medium = medium;
     }

    public String getCommand(){return this.command;}
    public String getWakeWord(){return this.wakeWord;}
    public String getPreArgs(){return this.preArgs;}
    public String getPostArgs(){return this.postArgs;}
    public Date getTimestamp(){return this.timestamp;}
    public String getMedium(){return this.medium;}
    public Location getLocation(){return this.location;}
    public void setLocation(Location location){this.location = location;}
    public String getAddress(){return this.address;}
    public void setAddress(String address){this.address = address;}
    public int getId(){return this.id;}
    public void setId(int id){this.id = id;}
}
