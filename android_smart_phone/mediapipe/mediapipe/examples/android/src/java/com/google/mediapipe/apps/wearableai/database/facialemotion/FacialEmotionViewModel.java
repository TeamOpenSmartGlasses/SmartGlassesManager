package com.google.mediapipe.apps.wearableai.database.facialemotion;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class FacialEmotionViewModel extends AndroidViewModel {

    private FacialEmotionRepository mRepository;
    private LiveData<List<FacialEmotion>> mAllFacialEmotions;
    private LiveData<FacialEmotion> mSelectedFacialEmotion;

    public FacialEmotionViewModel (Application application) {
        super(application);
        mRepository = new FacialEmotionRepository(application);
        mAllFacialEmotions = mRepository.getAllFacialEmotions();
    }

    public LiveData<List<FacialEmotion>> getAllFacialEmotions() {return mAllFacialEmotions;}
    public LiveData<FacialEmotion> getFacialEmotion(int id) {return mRepository.getFacialEmotion(id);}

    public void addFacialEmotion(String word, String medium) {
        FacialEmotionCreator.create(word, medium, getApplication().getApplicationContext(), mRepository);
    }
}
