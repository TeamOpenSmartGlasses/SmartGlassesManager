package com.smartglassesmanager.androidsmartphone.database.mediafile;

//originally from MXT: Memory Expansion Tools
//Jeremy Stairs (stairs1) and Cayden Pierce
//https://github.com/stairs1/memory-expansion-tools

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;

import java.util.concurrent.ExecutionException;
import java.lang.InterruptedException;

public class MediaFileViewModel extends AndroidViewModel {

    private MediaFileRepository mRepository;

    public MediaFileViewModel (Application application) {
        super(application);
        mRepository = new MediaFileRepository(application);
    }

    public MediaFileEntity getClosestMediaFileSnapshot(String mediaType, long timestamp){
        try {
            return mRepository.getClosestMediaFileSnapshot(mediaType, timestamp);
        } catch (ExecutionException | InterruptedException e){
            e.printStackTrace();
            return null;
        }
    }

//    public LiveData<List<MediaFileEntity>> getAllMediaFiles() {return mAllMediaFiles;}
//    public LiveData<MediaFileEntity> getMediaFile(int id) {return mRepository.getMediaFile(id);}
//
//    //mxt
//    public LiveData<List<Phrase>> getMxtCache() {return mRepository.getMediaFilePhrases("mxt", true);} //eventually, this will take an id we use to find the specific cache //this will get the phrases where the passed in values are true, so returns phrases
//
//
//    public LiveData<List<Phrase>> getTagBin(String tag) {return mRepository.getMediaFilePhrases("mxt", false, "tag", tag);} //eventually, this will take an id we use to find the specific cache //this will get the phrases where the passed in values are true, so returns phrases
//
//
////    public void addMediaFile(String word, String medium) {
//        MediaFileCreator.create(word, medium, getApplication().getApplicationContext(), mRepository);
//    }
}
