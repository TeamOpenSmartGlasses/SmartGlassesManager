package com.wearableintelligencesystem.androidsmartphone.database.person;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "PersonTable")
public class PersonEntity {

    //row id
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int id = 0;

    //id of the specific person
    @ColumnInfo(name = "personId")
    private Long personId;

    //argument key
    @ColumnInfo(name = "argKey")
    private String argKey;

    //argument value
    @ColumnInfo(name = "argValue")
    private String argValue;

    @NonNull
    @ColumnInfo(name = "timestamp")
    private long timestamp;

    //id of the media we extracted this information from (ussually image, face detection, but could be speaker diarization, text, a person's name mentioned in a transcript, etc.)
    @ColumnInfo(name = "mediaId")
    private Long mediaId;

    public PersonEntity(Long personId, @NonNull String argKey, @NonNull String argValue, @NonNull long timestamp, Long mediaId) {
        this.personId = personId;
        this.argKey = argKey;
        this.argValue = argValue;
        this.timestamp = timestamp;
        this.mediaId = mediaId;
    }

    public Long getPersonId(){return this.personId;}
    public void setPersonId(long personId){this.personId = personId;}
    public String getArgKey(){return this.argKey;}
    public String getArgValue(){return this.argValue;}
    public long getTimestamp(){return this.timestamp;}
    public Long getMediaId(){return this.mediaId;}
    public int getId(){return this.id;}
    public void setId(int id){this.id = id;}
}
