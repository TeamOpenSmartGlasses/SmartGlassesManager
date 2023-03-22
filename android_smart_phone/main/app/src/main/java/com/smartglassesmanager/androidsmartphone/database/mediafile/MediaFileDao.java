package com.smartglassesmanager.androidsmartphone.database.mediafile;

//originally from MXT: Memory Expansion Tools
//Jeremy Stairs (stairs1) and Cayden Pierce
//https://github.com/stairs1/memory-expansion-tools

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface MediaFileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(MediaFileEntity mediaFile);

    @Query("DELETE FROM MediaFileTable")
    void deleteAll();

    @Query("SELECT * from MediaFileTable WHERE mediaType=:mediaType ORDER BY abs(:timestamp - startTimestamp) LIMIT 1")
    MediaFileEntity getClosestMediaFileSnapshot(String mediaType, long timestamp);

//    @Query("SELECT * from MediaFileTable ORDER BY timestamp DESC")
//    LiveData<List<MediaFileEntity>> getAllMediaFiles();
//
//    @Query("SELECT * from MediaFileTable ORDER BY timestamp DESC")
//    List<MediaFileEntity> getAllMediaFilesSnapshot();
//    
//    @Query("SELECT * from MediaFileTable WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
//    List<MediaFileEntity> getMediaFileRange(Date startTime, Date endTime);
//
//    @Query("SELECT * from MediaFileTable WHERE commandName=:command ORDER BY timestamp DESC LIMIT 1")
//    MediaFileEntity getLatestCommand(String command);
//
    @Query("SELECT * FROM MediaFileTable WHERE ID = :id")
    MediaFileEntity getMediaFilebyId(long id);
//
//    @Query("SELECT * from MediaFileTable WHERE commandName=:commandName AND isMaster=:isMaster ORDER BY timestamp DESC")
//    LiveData<List<MediaFileEntity>> getMediaFiles(String commandName, boolean isMaster);
//
//    @Query("SELECT * FROM PhraseTable WHERE id IN (" +
//        "SELECT transcriptId from MediaFileTable WHERE commandName=:commandName AND isMaster=:isMaster" +
//    ") ORDER BY timestamp DESC")
//    LiveData<List<Phrase>> getMediaFilePhrases(String commandName, boolean isMaster);
//
//    @Query("SELECT * FROM PhraseTable WHERE id IN (" +
//        "SELECT transcriptId from MediaFileTable WHERE commandName=:commandName AND isMaster=:isMaster AND argKey=:argKey AND argValue=:argValue" +
//    ") ORDER BY timestamp DESC")
//    LiveData<List<Phrase>> getMediaFilePhrases(String commandName, boolean isMaster, String argKey, String argValue);
//
}
