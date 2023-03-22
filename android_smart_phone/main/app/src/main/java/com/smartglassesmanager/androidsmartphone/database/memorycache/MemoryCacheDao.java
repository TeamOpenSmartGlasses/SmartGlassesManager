package com.smartglassesmanager.androidsmartphone.database.memorycache;

//originally from MXT: Memory Expansion Tools
//Jeremy Stairs (stairs1) and Cayden Pierce
//https://github.com/stairs1/memory-expansion-tools

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MemoryCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(MemoryCache cache);

    @Query("UPDATE MemoryCacheTable SET cache_name = :name WHERE id = :id")
    void updateCacheName(long id, String name);

    //return id if the most recent cache has not yet been stopped. otherwise, return null
    //if returns null, then not active
    //if return something, then it's active
    @Query("SELECT id from (SELECT * from MemoryCacheTable ORDER BY startTimestamp DESC LIMIT 1) WHERE stopTimestamp IS NULL")
    Long getActiveCache();

    @Query("UPDATE MemoryCacheTable SET stopTimestamp = :stopTimestamp WHERE id = :id")
    void updateCacheStopTime(long id, long stopTimestamp);

    @Query("DELETE FROM MemoryCacheTable")
    void deleteAll();

    @Query("SELECT * from MemoryCacheTable ORDER BY startTimestamp DESC")
    LiveData<List<MemoryCache>> getAllMemoryCaches();

    @Query("SELECT * from MemoryCacheTable ORDER BY startTimestamp DESC")
    List<MemoryCache> getAllMemoryCachesSnapshot();

    @Query("SELECT * FROM MemoryCacheTable WHERE ID = :id")
    LiveData<MemoryCache> get_by_id(int id);
    
    @Query("SELECT * from MemoryCacheTable WHERE startTimestamp BETWEEN :startTime AND :endTime ORDER BY startTimestamp DESC")
    List<MemoryCache> getMemoryCacheRange(long startTime, long endTime);

    @Query("SELECT * FROM MemoryCacheTable ORDER BY ABS(:timestamp - startTimestamp) LIMIT 1")
    MemoryCache getByNearestTimestamp(long timestamp);

    @Query("SELECT * FROM MemoryCacheTable WHERE id IN (:ids)")
    List<MemoryCache> getMemoryCaches(List<Long> ids);
}
