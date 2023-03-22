package com.smartglassesmanager.androidsmartphone.database.phrase;

//originally from MXT: Memory Expansion Tools
//Jeremy Stairs (stairs1) and Cayden Pierce
//https://github.com/stairs1/memory-expansion-tools

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

import com.smartglassesmanager.androidsmartphone.database.WearableAiRoomDatabase;

public class PhraseRepository {

    private PhraseDao mPhraseDao;
    private LiveData<List<Phrase>> mAllPhrases;

    public PhraseRepository(Application application) {
        WearableAiRoomDatabase db = WearableAiRoomDatabase.getDatabase(application);
        mPhraseDao = db.phraseDao();
        mAllPhrases = mPhraseDao.getAllPhrases();
    }

    public void destroy(){
    }

    public LiveData<List<Phrase>> getAllPhrases() {
        return mAllPhrases;
    }

//    public List<Phrase> getAllPhrasesSnapshot() {
//        return mPhraseDao.getAllPhrasesSnapshot();
//    }

    public List<Phrase> getAllPhrasesSnapshot() throws ExecutionException, InterruptedException {

        Callable<List<Phrase>> callable = new Callable<List<Phrase>>() {
            @Override
            public List<Phrase> call() throws Exception {
                return mPhraseDao.getAllPhrasesSnapshot();
            }
        };

        Future<List<Phrase>> future = Executors.newSingleThreadExecutor().submit(callable);

        return future.get();
    }

    public long insert(Phrase phrase) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Callable<Long> insertCallable = () -> mPhraseDao.insert(phrase);
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

    public void update(long id, String words, Location location, String address) {
        WearableAiRoomDatabase.databaseWriteExecutor.execute(() -> {
            mPhraseDao.update(id, words, location, address);
        });
    }

    public LiveData<List<Phrase>> getPhraseRange(long startTime, long endTime) {
        return mPhraseDao.getPhraseRange(startTime, endTime);
    }

    public List<Phrase> getPhraseRangeSnapshot(long startTime, long endTime) {

        Callable<List<Phrase>> callable = new Callable<List<Phrase>>() {
            @Override
            public List<Phrase> call() throws Exception {
                return mPhraseDao.getPhraseRangeSnapshot(startTime, endTime);
            }
        };

        Future<List<Phrase>> future = Executors.newSingleThreadExecutor().submit(callable);

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e){
            e.printStackTrace();
        }
        return null;
    }

    public List<Phrase> getPhrases(List<Long> ids) throws ExecutionException, InterruptedException {

        Callable<List<Phrase>> callable = new Callable<List<Phrase>>() {
            @Override
            public List<Phrase> call() throws Exception {
                return mPhraseDao.getPhrases(ids);
            }
        };

        Future<List<Phrase>> future = Executors.newSingleThreadExecutor().submit(callable);

        return future.get();
    }



    public LiveData<Phrase> getPhrase(long id) {
        return mPhraseDao.get_by_id(id);
    }

    public Phrase getPhraseSnapshot(long id) {
        return mPhraseDao.getByIdSnapshot(id);
    }

    public Phrase getByNearestTimestamp(long timestamp) throws ExecutionException, InterruptedException {

        Callable<Phrase> callable = new Callable<Phrase>() {
            @Override
            public Phrase call() throws Exception {
                return mPhraseDao.getByNearestTimestamp(timestamp);
            }
        };

        Future<Phrase> future = Executors.newSingleThreadExecutor().submit(callable);

        return future.get();
    }

}
