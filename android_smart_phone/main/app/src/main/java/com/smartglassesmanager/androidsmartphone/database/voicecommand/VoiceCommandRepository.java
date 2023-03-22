package com.smartglassesmanager.androidsmartphone.database.voicecommand;

//originally from MXT: Memory Expansion Tools
//Jeremy Stairs (stairs1) and Cayden Pierce
//https://github.com/stairs1/memory-expansion-tools

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.lang.InterruptedException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.smartglassesmanager.androidsmartphone.database.phrase.Phrase;

import com.smartglassesmanager.androidsmartphone.database.WearableAiRoomDatabase;

public class VoiceCommandRepository {

    private VoiceCommandDao mVoiceCommandDao;
    private LiveData<List<VoiceCommandEntity>> mAllVoiceCommands;

    public VoiceCommandRepository(Application application) {
        WearableAiRoomDatabase db = WearableAiRoomDatabase.getDatabase(application);
        mVoiceCommandDao = db.voiceCommandDao();
        mAllVoiceCommands = mVoiceCommandDao.getAllVoiceCommands();
    }

    public void destroy(){
    }


    public LiveData<List<VoiceCommandEntity>> getAllVoiceCommands() {
        return mAllVoiceCommands;
    }

//    public List<<VoiceCommandEntity>> getAllVoiceCommandsSnapshot() {
//        return mVoiceCommandDao.getAllVoiceCommandsSnapshot();
//    }

    public List<VoiceCommandEntity> getAllVoiceCommandsSnapshot() throws ExecutionException, InterruptedException {

        Callable<List<VoiceCommandEntity>> callable = new Callable<List<VoiceCommandEntity>>() {
            @Override
            public List<VoiceCommandEntity> call() throws Exception {
                return mVoiceCommandDao.getAllVoiceCommandsSnapshot();
            }
        };

        Future<List<VoiceCommandEntity>> future = Executors.newSingleThreadExecutor().submit(callable);

        return future.get();
    }

    public long insert(VoiceCommandEntity voiceCommand) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Callable<Long> insertCallable = () -> mVoiceCommandDao.insert(voiceCommand);
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

    public LiveData<VoiceCommandEntity> getVoiceCommand(int id) {
        return mVoiceCommandDao.get_by_id(id);
    }

//    public LiveData<List<VoiceCommandEntity>> getVoiceCommands(String commandName, boolean isMaster) {
//        return mVoiceCommandDao.getVoiceCommands(commandName, isMaster);
//    }

    public LiveData<List<Phrase>> getVoiceCommandPhrases(String commandName, boolean isMaster) {
        return mVoiceCommandDao.getVoiceCommandPhrases(commandName, isMaster);
    }

    public List<Phrase> getVoiceCommandPhrasesSnapshot(String commandName, boolean isMaster) {

        Callable<List<Phrase>> callable = new Callable<List<Phrase>>() {
            @Override
            public List<Phrase> call() throws Exception {
                return mVoiceCommandDao.getVoiceCommandPhrasesSnapshot(commandName, isMaster);
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

    public LiveData<List<Phrase>> getVoiceCommandPhrases(String commandName, boolean isMaster, String argKey, String argValue) {
        return mVoiceCommandDao.getVoiceCommandPhrases(commandName, isMaster, argKey, argValue);
    }

    public LiveData<List<VoiceCommandEntity>> getVoiceCommands(String commandName, boolean isMaster) {
        return mVoiceCommandDao.getVoiceCommands(commandName, isMaster);
    }

    public LiveData<List<VoiceCommandEntity>> getVoiceCommands(String commandName, boolean isMaster, String argKey, String argValue) {
        return mVoiceCommandDao.getVoiceCommands(commandName, isMaster, argKey, argValue);
    }

    public VoiceCommandEntity getLatestCommand(String command) throws ExecutionException, InterruptedException {

        Callable<VoiceCommandEntity> callable = new Callable<VoiceCommandEntity>() {
            @Override
            public VoiceCommandEntity call() throws Exception {
                return mVoiceCommandDao.getLatestCommand(command);
            }
        };

        Future<VoiceCommandEntity> future = Executors.newSingleThreadExecutor().submit(callable);

        return future.get();
    }

}
