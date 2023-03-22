package com.smartglassesmanager.androidsmartphone.database.memorycache;

//originally from MXT: Memory Expansion Tools
//Jeremy Stairs (stairs1) and Cayden Pierce
//https://github.com/stairs1/memory-expansion-tools

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class MemoryCacheViewModel extends AndroidViewModel {

    private MemoryCacheRepository mRepository;
    private LiveData<List<MemoryCache>> mAllMemoryCaches;
    private LiveData<MemoryCache> mSelectedMemoryCache;

    public MemoryCacheViewModel (Application application) {
        super(application);
        mRepository = new MemoryCacheRepository(application);
//        mAllMemoryCaches = mRepository.getAllMemoryCaches();
    }

    public LiveData<List<MemoryCache>> getAllMemoryCaches() {return mAllMemoryCaches;}
//    public LiveData<MemoryCache> getMemoryCache(int id) {return mRepository.getMemoryCache(id);}
    //public List<MemoryCache> getMemoryCaches(List<Long> ids) {return mRepository.getMemoryCaches(ids);}

    public void newMemoryCache(long timestamp) {
        MemoryCacheCreator.create(timestamp, mRepository);
    }


    public List<MemoryCache> getAllCachesSnapshot() {
        return mRepository.getAllCachesSnapshot();
    }


    public void setCacheName(long id, String name) {
        mRepository.updateCacheName(id, name);
    }
}
