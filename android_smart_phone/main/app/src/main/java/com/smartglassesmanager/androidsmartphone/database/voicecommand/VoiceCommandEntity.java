package com.smartglassesmanager.androidsmartphone.database.voicecommand;

//originally from MXT: Memory Expansion Tools
//Jeremy Stairs (stairs1) and Cayden Pierce
//https://github.com/stairs1/memory-expansion-tools

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

//each voice command should be tied to a specific Phrase/Transcript id eventually, then we don't have to repeat information here and all data is interconnected
//thinking through the best way to store data, every single key:value pair will have its own row. So each command issued may have multiple rows, but command will only have ONE row with (isMaster == true), then, there may be subrows which hold more data for each command. I believe this is a good way of doing it, it may be a bit redundant, but it allows storing a wide variety of disparate voice commands into a single table
@Entity(tableName = "VoiceCommandTable")
public class VoiceCommandEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int id = 0;

    //the name of the command, this specified the code that actually ran
    @NonNull
    @ColumnInfo(name = "commandName")
    private String commandName;

    //what the user said to cause this command to run
    @NonNull
    @ColumnInfo(name = "commandSpoken")
    private String commandSpoken;

    @NonNull
    @ColumnInfo(name = "wakeWord")
    private String wakeWord;

    //is this the main command, or sub commands to the main one?
    @NonNull
    @ColumnInfo(name = "isMaster")
    private boolean isMaster;

    //argument key
    @ColumnInfo(name = "argKey")
    private String argKey;

    //argument value
    @ColumnInfo(name = "argValue")
    private String argValue;

    @ColumnInfo(name = "preArgs")
    private String preArgs;

    @ColumnInfo(name = "postArgs")
    private String postArgs;

    @NonNull
    @ColumnInfo(name = "timestamp")
    private long timestamp;

    @ColumnInfo(name = "medium")
    private String medium;

    @NonNull
    @ColumnInfo(name = "transcriptId")
    private long transcriptId;

    public VoiceCommandEntity(@NonNull String commandName, @NonNull String commandSpoken, @NonNull String wakeWord, @NonNull boolean isMaster, @NonNull String argKey, @NonNull String argValue, @NonNull long timestamp, String preArgs, String postArgs, @NonNull String medium, @NonNull long transcriptId) {
        this.commandName = commandName;
        this.commandSpoken = commandSpoken;
        this.wakeWord = wakeWord;
        this.isMaster = isMaster;
        this.argKey = argKey;
        this.argValue = argValue;
        this.timestamp = timestamp;
        this.medium = medium;
        this.transcriptId = transcriptId;
        this.preArgs = preArgs;
        this.postArgs = postArgs;
    }

    public String getCommandName(){return this.commandName;}
    public String getCommandSpoken(){return this.commandSpoken;}
    public String getWakeWord(){return this.wakeWord;}
    public String getArgKey(){return this.argKey;}
    public String getArgValue(){return this.argValue;}
    public boolean getIsMaster(){return this.isMaster;}
    public long getTimestamp(){return this.timestamp;}
    public String getMedium(){return this.medium;}
    public long getTranscriptId(){return this.transcriptId;}
    public int getId(){return this.id;}
    public void setId(int id){this.id = id;}

    public String getPreArgs() {
        return preArgs;
    }

    public void setPreArgs(String preArgs) {
        this.preArgs = preArgs;
    }

    public String getPostArgs() {
        return postArgs;
    }

    public void setPostArgs(String postArgs) {
        this.postArgs = postArgs;
    }

}
