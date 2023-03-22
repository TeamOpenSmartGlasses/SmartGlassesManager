package com.smartglassesmanager.androidsmartphone.database;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.smartglassesmanager.androidsmartphone.database.mediafile.MediaFileDao;
import com.smartglassesmanager.androidsmartphone.database.mediafile.MediaFileEntity;
import com.smartglassesmanager.androidsmartphone.database.memorycache.MemoryCache;
import com.smartglassesmanager.androidsmartphone.database.memorycache.MemoryCacheDao;
import com.smartglassesmanager.androidsmartphone.database.memorycache.MemoryCacheTimes;
import com.smartglassesmanager.androidsmartphone.database.memorycache.MemoryCacheTimesDao;
import com.smartglassesmanager.androidsmartphone.database.phrase.Phrase;
import com.smartglassesmanager.androidsmartphone.database.phrase.PhraseDao;
import com.smartglassesmanager.androidsmartphone.database.voicecommand.VoiceCommandDao;
import com.smartglassesmanager.androidsmartphone.database.voicecommand.VoiceCommandEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Phrase.class, VoiceCommandEntity.class, MediaFileEntity.class, MemoryCache.class, MemoryCacheTimes.class}, version = 3, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class WearableAiRoomDatabase extends RoomDatabase {
    private static final String TAG = "WearableAi_WearableAiRoomDatabase";

    private static volatile WearableAiRoomDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public abstract PhraseDao phraseDao();
    public abstract VoiceCommandDao voiceCommandDao();
    public abstract MediaFileDao mediaFileDao();
    public abstract MemoryCacheDao memoryCacheDao();
    public abstract MemoryCacheTimesDao memoryCacheTimesDao();

    public static WearableAiRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (WearableAiRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), WearableAiRoomDatabase.class, "wearableai_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public static void destroy(){
        Log.d(TAG, "onDestroy: --------");
        if(INSTANCE!=null){
          if(INSTANCE.isOpen()) {
            INSTANCE.close();
          }
          INSTANCE=null;
        }
    }

}
