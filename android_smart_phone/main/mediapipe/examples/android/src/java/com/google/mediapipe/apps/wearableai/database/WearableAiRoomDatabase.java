package com.google.mediapipe.apps.wearableai.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.mediapipe.apps.wearableai.database.Converters;

import com.google.mediapipe.apps.wearableai.database.phrase.PhraseDao;
import com.google.mediapipe.apps.wearableai.database.phrase.Phrase;

import com.google.mediapipe.apps.wearableai.database.facialemotion.FacialEmotionDao;
import com.google.mediapipe.apps.wearableai.database.facialemotion.FacialEmotion;

import com.google.mediapipe.apps.wearableai.database.voicecommand.VoiceCommandDao;
import com.google.mediapipe.apps.wearableai.database.voicecommand.VoiceCommandEntity;

@Database(entities = {FacialEmotion.class, Phrase.class, VoiceCommandEntity.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class WearableAiRoomDatabase extends RoomDatabase {

    private static volatile WearableAiRoomDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public abstract PhraseDao phraseDao();
    public abstract FacialEmotionDao facialEmotionDao();
    public abstract VoiceCommandDao voiceCommandDao();

    public static WearableAiRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (WearableAiRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), WearableAiRoomDatabase.class, "phrase_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }

}
