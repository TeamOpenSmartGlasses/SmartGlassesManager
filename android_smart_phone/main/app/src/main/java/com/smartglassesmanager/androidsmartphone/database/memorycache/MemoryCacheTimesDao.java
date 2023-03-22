package com.smartglassesmanager.androidsmartphone.database.memorycache;

//originally from MXT: Memory Expansion Tools
//Jeremy Stairs (stairs1) and Cayden Pierce
//https://github.com/stairs1/memory-expansion-tools

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface MemoryCacheTimesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(MemoryCacheTimes cache);

    @Query("DELETE FROM MemoryCacheTimesTable")
    void deleteAll();

    //return cache_id if the last command was a start. Otherwise, return null, because no cache is active
    @Query("SELECT cache_id from (SELECT * from MemoryCacheTimesTable WHERE timestamp <= :timestamp ORDER BY timestamp DESC LIMIT 1) WHERE start = 1")
    Long getActiveCache(long timestamp);

//    @Query("SELECT * from MemoryCacheTimesTable ORDER BY timestamp DESC")
//    LiveData<List<MemoryCache>> getAllMemoryCaches();
//
//    @Query("SELECT * from MemoryCacheTimesTable ORDER BY timestamp DESC")
//    List<MemoryCache> getAllMemoryCachesSnapshot();
//
//    @Query("SELECT * FROM MemoryCacheTimesTable WHERE ID = :id")
//    LiveData<MemoryCache> get_by_id(int id);
//
//    @Query("SELECT * from MemoryCacheTimesTable WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
//    List<MemoryCache> getMemoryCacheRange(long startTime, long endTime);
//
//    @Query("SELECT * FROM MemoryCacheTimesTable ORDER BY ABS(:timestamp - timestamp) LIMIT 1")
//    MemoryCache getByNearestTimestamp(long timestamp);
//
//    @Query("SELECT * FROM MemoryCacheTimesTable WHERE id IN (:ids)")
//    List<MemoryCache> getMemoryCaches(List<Long> ids);
}
