package com.wearableintelligencesystem.androidsmartphone.database.person;

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

import com.wearableintelligencesystem.androidsmartphone.database.phrase.Phrase;

import com.wearableintelligencesystem.androidsmartphone.database.WearableAiRoomDatabase;

public class PersonRepository {

    private PersonDao mPersonDao;
    private LiveData<List<PersonEntity>> mAllPersons;

    public PersonRepository(Application application) {
        WearableAiRoomDatabase db = WearableAiRoomDatabase.getDatabase(application);
        mPersonDao = db.personDao();
        mAllPersons = mPersonDao.getAllPersons();
    }

    public void destroy(){
    }


    public LiveData<List<PersonEntity>> getAllPersons() {
        return mAllPersons;
    }

    public List<PersonEntity> getAllPersonsSnapshot(){

        Callable<List<PersonEntity>> callable = new Callable<List<PersonEntity>>() {
            @Override
            public List<PersonEntity> call() throws Exception {
                return mPersonDao.getAllPersonsSnapshot();
            }
        };

        Future<List<PersonEntity>> future = Executors.newSingleThreadExecutor().submit(callable);

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e){
            e.printStackTrace();
            return null;
        }
    }

    public List<PersonEntity> getAllPersonsSnapshotTimePeriod(long startTime, long endTime) throws ExecutionException, InterruptedException {

        Callable<List<PersonEntity>> callable = new Callable<List<PersonEntity>>() {
            @Override
            public List<PersonEntity> call() throws Exception {
                return mPersonDao.getAllPersonsSnapshotTimePeriod(startTime, endTime);
            }
        };

        Future<List<PersonEntity>> future = Executors.newSingleThreadExecutor().submit(callable);

        return future.get();
    }

    public List<PersonEntity> getAllKnownPersonsNamesSnapshot() throws ExecutionException, InterruptedException {

        Callable<List<PersonEntity>> callable = new Callable<List<PersonEntity>>() {
            @Override
            public List<PersonEntity> call() throws Exception {
                return mPersonDao.getAllKnownPersonsNamesSnapshot();
            }
        };

        Future<List<PersonEntity>> future = Executors.newSingleThreadExecutor().submit(callable);

        return future.get();
    }

    public long insert(PersonEntity person) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Callable<Long> insertCallable = () -> mPersonDao.insert(person);
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

    public LiveData<PersonEntity> getPerson(int id) {
        return mPersonDao.get_by_id(id);
    }

    //return all new_face_id rows that were dropped as too bad for face recognition
    public LiveData<List<PersonEntity>> getBadForFaceRec() {
        return mPersonDao.getBadForFaceRec();
    }

    public String getPersonsName(long id) throws ExecutionException, InterruptedException {

       try{
            Callable<PersonEntity> callable = new Callable<PersonEntity>() {
                @Override
                public PersonEntity call() throws Exception {
                    return mPersonDao.getPersonsName(id);
                }
            };

            Future<PersonEntity> future = Executors.newSingleThreadExecutor().submit(callable);
            PersonEntity person = future.get();
            if (person == null){
                //if null, we should check if the person exists but their name was changed
                Long newId = getChangedPersonId(id);
                //call this function with the new id and return that value
                return getPersonsName(newId);
            } else {
                String name = person.getArgValue();
                return name;
            }
        } catch (InterruptedException | ExecutionException e){
            e.printStackTrace();
            return null;
        }
    }

    public Long getChangedPersonId(long id) throws ExecutionException, InterruptedException {

       try{
            Callable<PersonEntity> callable = new Callable<PersonEntity>() {
                @Override
                public PersonEntity call() throws Exception {
                    return mPersonDao.getChangedPersonId(id);
                }
            };

            Future<PersonEntity> future = Executors.newSingleThreadExecutor().submit(callable);
            PersonEntity person = future.get();
            if (person == null){
                return null;
            } else {
                Long newId = Long.parseLong(person.getArgValue());
                return newId;
            }
        } catch (InterruptedException | ExecutionException e){
            e.printStackTrace();
            return null;
        }
    }


    public PersonEntity getPersonLastSeen(long id) throws ExecutionException, InterruptedException {

        Callable<PersonEntity> callable = new Callable<PersonEntity>() {
            @Override
            public PersonEntity call() throws Exception {
                return mPersonDao.getPersonLastSeen(id);
            }
        };

        Future<PersonEntity> future = Executors.newSingleThreadExecutor().submit(callable);

        return future.get();
    }

    //this just updates the personId to match the row id, for a new person
    public void updatePersonId(long personId){
        WearableAiRoomDatabase.databaseWriteExecutor.execute(() -> {
            mPersonDao.updatePersonId(personId);
        });
    }

    //this is used to change the personId to a different, GIVEN person id, so that a face rec we thought was a new person can be updated as a known person
    public void changePersonId(long unknownPersonId, long knownPersonId){
        WearableAiRoomDatabase.databaseWriteExecutor.execute(() -> {
            mPersonDao.changePersonId(unknownPersonId, knownPersonId);
        });
    }

    public void setPersonConfirmedUnknown(long personId){
        WearableAiRoomDatabase.databaseWriteExecutor.execute(() -> {
            mPersonDao.updatePersonName("confirmed_unknown", personId);
        });
    }

    public void updatePersonName(long personId, String name){
        WearableAiRoomDatabase.databaseWriteExecutor.execute(() -> {
            mPersonDao.updatePersonName(name, personId);
        });
    }

    public void deletePerson(long personId){
        WearableAiRoomDatabase.databaseWriteExecutor.execute(() -> {
            mPersonDao.deletePerson(personId);
        });
    }

    public List<PersonEntity> getUnknownPersonsSnapshot() throws ExecutionException, InterruptedException {

        Callable<List<PersonEntity>> callable = new Callable<List<PersonEntity>>() {
            @Override
            public List<PersonEntity> call() throws Exception {
                return mPersonDao.getUnknownPersonsSnapshot();
            }
        };

        Future<List<PersonEntity>> future = Executors.newSingleThreadExecutor().submit(callable);

        return future.get();
    }
}
