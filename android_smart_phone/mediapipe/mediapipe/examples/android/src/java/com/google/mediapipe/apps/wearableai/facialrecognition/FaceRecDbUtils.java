package com.google.mediapipe.apps.wearableai.facialrecognition;

//android
import android.content.Context;
import android.util.Log;
import android.content.Context;
import android.os.Handler;
import java.io.IOException;
import java.lang.InterruptedException;
import java.util.Arrays;

import com.google.mediapipe.apps.wearableai.database.person.PersonRepository;
import com.google.mediapipe.apps.wearableai.database.person.PersonCreator;
import com.google.mediapipe.apps.wearableai.database.person.PersonEntity;

import java.util.List;

import java.util.concurrent.ExecutionException;
import java.lang.InterruptedException;

//rxjava
//import com.google.mediapipe.apps.wearableai.comms.MessageTypes;
//import io.reactivex.rxjava3.disposables.Disposable;
//import io.reactivex.rxjava3.subjects.PublishSubject;

public class FaceRecDbUtils {
    public String TAG = "WearableAi_FaceRecDbUtils";

    private Context mContext;

    private PersonRepository mPersonRepository = null;

    public FaceRecDbUtils(Context context, PersonRepository mPersonRepository){
        mContext = context;

        //to save/modify db
        this.mPersonRepository = mPersonRepository;
    }

    //take a person row id and set it as a bad image
    public void setBadImage(Long id, long timestamp, long mediaId){
        //delete the person from our database
        mPersonRepository.deletePerson(id);

        //set that it was bad image for face rec so remove this from the face rec object in face rec api
        //do this after deletePerson so the only row mentioning this person exists to tell our FaceRecApi
        PersonCreator.create(id, "meta", "bad_for_face_rec", timestamp, mediaId, mPersonRepository);
    }

    //take a person id and set them as a confirmed unknown
    public void setConfirmedUnknown(long personId){
        mPersonRepository.setPersonConfirmedUnknown(personId);
    }

    //take an unknown person id and set them to a new name
    public void newFaceAdd(long personId, String name){
        mPersonRepository.updatePersonName(personId, name);
    }

    //take an unknown person id and set them to a person we already know
    public void existingFaceAdd(long unknownPersonId, long knownPersonId){
        mPersonRepository.changePersonId(unknownPersonId, knownPersonId);
    }

    //we need to find if the name is a match, then call proper function based on whether it's a match or not
    public void faceAdd(long id, String personName){
        //get all names to compare against
        List<PersonEntity> allNames = null;
        try{
             allNames = mPersonRepository.getAllKnownPersonsNamesSnapshot();
        } catch (ExecutionException | InterruptedException e){
            e.printStackTrace();
        }
        boolean personExists = false;
        Long knownPersonId = null;
        Log.d(TAG, "List of all names in system: ");
        if (allNames != null){ //if it's null, then person doesn't exist
            for (int i = 0; i < allNames.size(); i++){
                Log.d(TAG, allNames.get(i).getArgValue());
            }

            for (int i = 0; i < allNames.size(); i++){
                if (personName.equals(allNames.get(i).getArgValue())){
                    personExists = true;
                    knownPersonId = allNames.get(i).getPersonId();
                    break;
                }
            }
        }

        //pass to proper person adder function - based on whether or not the name is already in our database
        if (personExists){
            Log.d(TAG, "running existing face add");
            existingFaceAdd(id, knownPersonId);
        } else {
            Log.d(TAG, "running new face add");
            newFaceAdd(id, personName);
        }
    }
}
