package com.google.mediapipe.apps.wearableai.database.phrase;

//originally from MXT: Memory Expansion Tools
//Jeremy Stairs (stairs1) and Cayden Pierce
//https://github.com/stairs1/memory-expansion-tools

import android.location.Location;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.lang.Integer;

import java.util.List;

@Dao
public interface PhraseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Phrase phrase);

    @Query("UPDATE PhraseTable SET location = :location, address = :address WHERE id = :id")
    void update(long id, Location location, String address);

    @Query("DELETE FROM PhraseTable")
    void deleteAll();

    @Query("SELECT * from PhraseTable ORDER BY timestamp DESC")
    LiveData<List<Phrase>> getAllPhrases();

    @Query("SELECT * from PhraseTable ORDER BY timestamp DESC")
    List<Phrase> getAllPhrasesSnapshot();

    @Query("SELECT * FROM PhraseTable WHERE ID = :id")
    LiveData<Phrase> get_by_id(int id);
    
    @Query("SELECT * from PhraseTable WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    List<Phrase> getPhraseRange(long startTime, long endTime);

    @Query("SELECT * FROM PhraseTable ORDER BY ABS(:timestamp - timestamp) LIMIT 1")
    Phrase getByNearestTimestamp(long timestamp);

    @Query("SELECT * FROM PhraseTable WHERE id IN (:ids)")
    List<Phrase> getPhrases(List<Long> ids);
}
