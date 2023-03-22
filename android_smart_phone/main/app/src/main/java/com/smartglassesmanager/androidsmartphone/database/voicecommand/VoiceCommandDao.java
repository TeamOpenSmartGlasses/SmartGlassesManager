package com.smartglassesmanager.androidsmartphone.database.voicecommand;

//originally from MXT: Memory Expansion Tools
//Jeremy Stairs (stairs1) and Cayden Pierce
//https://github.com/stairs1/memory-expansion-tools

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.Date;

import java.util.List;

import com.smartglassesmanager.androidsmartphone.database.phrase.Phrase;

@Dao
public interface VoiceCommandDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(VoiceCommandEntity voiceCommand);

    @Query("DELETE FROM VoiceCommandTable")
    void deleteAll();

    @Query("SELECT * from VoiceCommandTable ORDER BY timestamp DESC")
    LiveData<List<VoiceCommandEntity>> getAllVoiceCommands();

    @Query("SELECT * from VoiceCommandTable ORDER BY timestamp DESC")
    List<VoiceCommandEntity> getAllVoiceCommandsSnapshot();
    
    @Query("SELECT * from VoiceCommandTable WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    List<VoiceCommandEntity> getVoiceCommandRange(Date startTime, Date endTime);

//    @Query("SELECT * from VoiceCommandTable WHERE isMaster = :isMaster AND argKey = :argKey ORDER BY timestamp DESC")
//    List<VoiceCommandEntity> getVoiceCommands(boolean isMaster, String argKey);

    @Query("SELECT * from VoiceCommandTable WHERE commandName=:command ORDER BY timestamp DESC LIMIT 1")
    VoiceCommandEntity getLatestCommand(String command);

    @Query("SELECT * FROM VoiceCommandTable WHERE ID = :id")
    LiveData<VoiceCommandEntity> get_by_id(int id);

    @Query("SELECT * from VoiceCommandTable WHERE commandName=:commandName AND isMaster=:isMaster ORDER BY timestamp DESC")
    LiveData<List<VoiceCommandEntity>> getVoiceCommands(String commandName, boolean isMaster);

    @Query("SELECT * from VoiceCommandTable WHERE commandName=:commandName AND isMaster=:isMaster AND argKey = :argKey AND argValue = :argValue ORDER BY timestamp DESC")
    LiveData<List<VoiceCommandEntity>> getVoiceCommands(String commandName, boolean isMaster, String argKey, String argValue);

    @Query("SELECT * FROM PhraseTable WHERE id IN (" +
        "SELECT transcriptId from VoiceCommandTable WHERE commandName=:commandName AND isMaster=:isMaster" +
    ") ORDER BY timestamp DESC")
    LiveData<List<Phrase>> getVoiceCommandPhrases(String commandName, boolean isMaster);

    @Query("SELECT * FROM PhraseTable WHERE id IN (" +
            "SELECT transcriptId from VoiceCommandTable WHERE commandName=:commandName AND isMaster=:isMaster" +
            ") ORDER BY timestamp DESC")
    List<Phrase> getVoiceCommandPhrasesSnapshot(String commandName, boolean isMaster);

    @Query("SELECT * FROM PhraseTable WHERE id IN (" +
        "SELECT transcriptId from VoiceCommandTable WHERE commandName=:commandName AND isMaster=:isMaster AND argKey=:argKey AND argValue=:argValue" +
    ") ORDER BY timestamp DESC")
    LiveData<List<Phrase>> getVoiceCommandPhrases(String commandName, boolean isMaster, String argKey, String argValue);

}
