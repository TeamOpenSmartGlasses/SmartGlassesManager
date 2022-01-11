package com.wearableintelligencesystem.androidsmartphone.database.facialemotion;

import android.app.Application;
import android.location.Location;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.lang.InterruptedException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.wearableintelligencesystem.androidsmartphone.database.WearableAiRoomDatabase;

public class FacialEmotionRepository {

    private FacialEmotionDao mFacialEmotionDao;
    private LiveData<List<FacialEmotion>> mAllFacialEmotions;

    public FacialEmotionRepository(Application application) {
        WearableAiRoomDatabase db = WearableAiRoomDatabase.getDatabase(application);
        mFacialEmotionDao = db.facialEmotionDao();
        mAllFacialEmotions = mFacialEmotionDao.getAllFacialEmotions();
    }

    public void destroy(){
    }

    public LiveData<List<FacialEmotion>> getAllFacialEmotions() {
        return mAllFacialEmotions;
    }

    public List<FacialEmotion> getAllFacialEmotionsSnapshot() throws ExecutionException, InterruptedException {

        Callable<List<FacialEmotion>> callable = new Callable<List<FacialEmotion>>() {
            @Override
            public List<FacialEmotion> call() throws Exception {
                return mFacialEmotionDao.getAllFacialEmotionsSnapshot();
            }
        };

        Future<List<FacialEmotion>> future = Executors.newSingleThreadExecutor().submit(callable);

        return future.get();
    }

    public List<FacialEmotion> getFacialEmotionsRange(long startTime, long endTime) throws ExecutionException, InterruptedException {

        Callable<List<FacialEmotion>> callable = new Callable<List<FacialEmotion>>() {
            @Override
            public List<FacialEmotion> call() throws Exception {
                return mFacialEmotionDao.getFacialEmotionsRange(startTime, endTime);
            }
        };

        Future<List<FacialEmotion>> future = Executors.newSingleThreadExecutor().submit(callable);

        return future.get();
    }


    public long insert(FacialEmotion facialEmotion) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Callable<Long> insertCallable = () -> mFacialEmotionDao.insert(facialEmotion);
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

    public void update(long id, Location location, String address) {
        WearableAiRoomDatabase.databaseWriteExecutor.execute(() -> {
            mFacialEmotionDao.update(id, location, address);
        });
    }

    public LiveData<FacialEmotion> getFacialEmotion(int id) {
        return mFacialEmotionDao.get_by_id(id);
    }
}
