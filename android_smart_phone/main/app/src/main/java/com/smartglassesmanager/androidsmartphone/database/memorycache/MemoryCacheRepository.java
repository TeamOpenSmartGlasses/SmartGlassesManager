package com.smartglassesmanager.androidsmartphone.database.memorycache;

//originally from MXT: Memory Expansion Tools
//Jeremy Stairs (stairs1) and Cayden Pierce
//https://github.com/stairs1/memory-expansion-tools

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.smartglassesmanager.androidsmartphone.database.WearableAiRoomDatabase;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MemoryCacheRepository {
    private final String TAG = "WearableAi_MemoryCacheRepository";

    private MemoryCacheDao mMemoryCacheDao;
    private MemoryCacheTimesDao mMemoryCacheTimesDao;
    private LiveData<List<MemoryCache>> mAllMemoryCaches;

    public MemoryCacheRepository(Application application) {
        WearableAiRoomDatabase db = WearableAiRoomDatabase.getDatabase(application);
        mMemoryCacheDao = db.memoryCacheDao();
        mMemoryCacheTimesDao = db.memoryCacheTimesDao();
    }

    public void destroy() {
    }

    public void updateCacheName(long id, String name) {
        WearableAiRoomDatabase.databaseWriteExecutor.execute(() -> {
            mMemoryCacheDao.updateCacheName(id, name);
        });
    }

    public long insert(MemoryCache cache) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Callable<Long> insertCallable = () -> mMemoryCacheDao.insert(cache);
        Future<Long> future = executorService.submit(insertCallable);
        long rowId = 0;
        try{
            rowId = future.get();
        }
        catch (InterruptedException | ExecutionException e){
            e.printStackTrace();
        }
        return rowId;
    }

    public long addCacheTime(MemoryCacheTimes cacheTime) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Callable<Long> insertCallable = () -> mMemoryCacheTimesDao.insert(cacheTime);
        Future<Long> future = executorService.submit(insertCallable);
        long rowId = 0;
        try{
            rowId = future.get();
        }
        catch (InterruptedException | ExecutionException e){
            e.printStackTrace();
        }
        return rowId;
    }

    public Long getActiveCache(){
        return mMemoryCacheDao.getActiveCache();
    }

    public List<MemoryCache> getAllCachesSnapshot() {

        Callable<List<MemoryCache>> callable = new Callable<List<MemoryCache>>() {
            @Override
            public List<MemoryCache> call() throws Exception {
                return mMemoryCacheDao.getAllMemoryCachesSnapshot();
            }
        };

        Future<List<MemoryCache>> future = Executors.newSingleThreadExecutor().submit(callable);

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e){
            e.printStackTrace();
            return null;
        }
    }

    public void updateCacheStopTime(long id, long stopTimestamp){
        mMemoryCacheDao.updateCacheStopTime(id, stopTimestamp);
    }
}
