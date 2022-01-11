package com.google.mediapipe.apps.wearableai.database.phrase;

//originally from MXT: Memory Expansion Tools
//Jeremy Stairs (stairs1) and Cayden Pierce
//https://github.com/stairs1/memory-expansion-tools

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class PhraseViewModel extends AndroidViewModel {

    private PhraseRepository mRepository;
    private LiveData<List<Phrase>> mAllPhrases;
    private LiveData<Phrase> mSelectedPhrase;

    public PhraseViewModel (Application application) {
        super(application);
        mRepository = new PhraseRepository(application);
        mAllPhrases = mRepository.getAllPhrases();
    }

    public LiveData<List<Phrase>> getAllPhrases() {return mAllPhrases;}
    public LiveData<Phrase> getPhrase(int id) {return mRepository.getPhrase(id);}
    //public List<Phrase> getPhrases(List<Long> ids) {return mRepository.getPhrases(ids);}

    public void addPhrase(String word, String medium) {
        PhraseCreator.create(word, medium, getApplication().getApplicationContext(), mRepository);
    }
}
