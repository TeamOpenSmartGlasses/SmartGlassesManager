package com.wearableintelligencesystem.androidsmartphone.database;

import android.util.Log;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.wearableintelligencesystem.androidsmartphone.database.Converters;

import com.wearableintelligencesystem.androidsmartphone.database.memorycache.MemoryCache;
import com.wearableintelligencesystem.androidsmartphone.database.memorycache.MemoryCacheDao;
import com.wearableintelligencesystem.androidsmartphone.database.memorycache.MemoryCacheTimes;
import com.wearableintelligencesystem.androidsmartphone.database.memorycache.MemoryCacheTimesDao;
import com.wearableintelligencesystem.androidsmartphone.database.phrase.PhraseDao;
import com.wearableintelligencesystem.androidsmartphone.database.phrase.Phrase;

import com.wearableintelligencesystem.androidsmartphone.database.facialemotion.FacialEmotionDao;
import com.wearableintelligencesystem.androidsmartphone.database.facialemotion.FacialEmotion;

import com.wearableintelligencesystem.androidsmartphone.database.voicecommand.VoiceCommandDao;
import com.wearableintelligencesystem.androidsmartphone.database.voicecommand.VoiceCommandEntity;

import com.wearableintelligencesystem.androidsmartphone.database.mediafile.MediaFileDao;
import com.wearableintelligencesystem.androidsmartphone.database.mediafile.MediaFileEntity;

import com.wearableintelligencesystem.androidsmartphone.database.person.PersonDao;
import com.wearableintelligencesystem.androidsmartphone.database.person.PersonEntity;

@Database(entities = {FacialEmotion.class, Phrase.class, VoiceCommandEntity.class, MediaFileEntity.class, PersonEntity.class, MemoryCache.class, MemoryCacheTimes.class}, version = 2, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class WearableAiRoomDatabase extends RoomDatabase {
    private static final String TAG = "WearableAi_WearableAiRoomDatabase";

    private static volatile WearableAiRoomDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public abstract PhraseDao phraseDao();
    public abstract FacialEmotionDao facialEmotionDao();
    public abstract VoiceCommandDao voiceCommandDao();
    public abstract MediaFileDao mediaFileDao();
    public abstract PersonDao personDao();
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
