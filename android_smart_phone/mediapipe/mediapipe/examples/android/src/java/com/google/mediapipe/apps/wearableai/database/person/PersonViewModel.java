package com.google.mediapipe.apps.wearableai.database.person;

//originally from MXT: Memory Expansion Tools
//Jeremy Stairs (stairs1) and Cayden Pierce
//https://github.com/stairs1/memory-expansion-tools

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.google.mediapipe.apps.wearableai.database.phrase.Phrase;

import java.util.List;

import java.util.concurrent.ExecutionException;

public class PersonViewModel extends AndroidViewModel {

    private PersonRepository mRepository;
    private LiveData<List<PersonEntity>> mAllPersons;
    private LiveData<PersonEntity> mSelectedPerson;

    public PersonViewModel (Application application) {
        super(application);
        mRepository = new PersonRepository(application);
    }

    public LiveData<PersonEntity> getPerson(int id) {return mRepository.getPerson(id);}

//    public String getPersonsName(long id) { //get the name of the person given their id
//        try{
//            return mRepository.getPersonsName(id).getArgValue();
//        } catch (ExecutionException | InterruptedException e){
//            e.printStackTrace();
//            return null;
//        }
//    }   

    public PersonEntity getPersonLastSeen(long id) { //get the timestamp a person was last seen given their id
        try{
            return mRepository.getPersonLastSeen(id);
        } catch (ExecutionException | InterruptedException e){
            e.printStackTrace();
            return null;
        }

    }

}
