package com.wearableintelligencesystem.androidsmartphone.database.facialemotion;

import android.location.Location;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FacialEmotionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(FacialEmotion facialEmotion);

    @Query("UPDATE FacialEmotionTable SET location = :location, address = :address WHERE id = :id")
    void update(long id, Location location, String address);

    @Query("DELETE FROM FacialEmotionTable")
    void deleteAll();

    @Query("SELECT * from FacialEmotionTable ORDER BY timestamp DESC")
    LiveData<List<FacialEmotion>> getAllFacialEmotions();

    @Query("SELECT * from FacialEmotionTable ORDER BY timestamp DESC")
    List<FacialEmotion> getAllFacialEmotionsSnapshot();

    @Query("SELECT * from FacialEmotionTable WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    List<FacialEmotion> getFacialEmotionsRange(long startTime, long endTime);

    @Query("SELECT * FROM FacialEmotionTable WHERE ID = :id")
    LiveData<FacialEmotion> get_by_id(int id);
}
