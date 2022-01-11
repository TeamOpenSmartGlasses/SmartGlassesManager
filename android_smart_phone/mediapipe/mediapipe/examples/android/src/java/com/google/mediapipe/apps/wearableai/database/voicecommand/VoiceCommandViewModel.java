package com.google.mediapipe.apps.wearableai.database.voicecommand;

//originally from MXT: Memory Expansion Tools
//Jeremy Stairs (stairs1) and Cayden Pierce
//https://github.com/stairs1/memory-expansion-tools

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.google.mediapipe.apps.wearableai.database.phrase.Phrase;

import java.util.List;

public class VoiceCommandViewModel extends AndroidViewModel {

    private VoiceCommandRepository mRepository;
    private LiveData<List<VoiceCommandEntity>> mAllVoiceCommands;
    private LiveData<VoiceCommandEntity> mSelectedVoiceCommand;

    public VoiceCommandViewModel (Application application) {
        super(application);
        mRepository = new VoiceCommandRepository(application);
        mAllVoiceCommands = mRepository.getAllVoiceCommands();
    }

    public LiveData<List<VoiceCommandEntity>> getAllVoiceCommands() {return mAllVoiceCommands;}
    public LiveData<VoiceCommandEntity> getVoiceCommand(int id) {return mRepository.getVoiceCommand(id);}

    //mxt
    public LiveData<List<Phrase>> getMxtCache() {return mRepository.getVoiceCommandPhrases("save speech", true);} //eventually, this will take an id we use to find the specific cache //this will get the phrases where the passed in values are true, so returns phrases


    public LiveData<List<Phrase>> getTagBin(String tag) {return mRepository.getVoiceCommandPhrases("save speech", false, "tag", tag);} //eventually, this will take an id we use to find the specific cache //this will get the phrases where the passed in values are true, so returns phrases


//    public void addVoiceCommand(String word, String medium) {
//        VoiceCommandCreator.create(word, medium, getApplication().getApplicationContext(), mRepository);
//    }
}
