package com.google.mediapipe.apps.wearableai.database.phrase;

//originally from MXT: Memory Expansion Tools
//Jeremy Stairs (stairs1) and Cayden Pierce
//https://github.com/stairs1/memory-expansion-tools

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Phrase.class}, version = 1, exportSchema = false)
@TypeConverters({PhraseConverters.class})
public abstract class PhraseRoomDatabase extends RoomDatabase {

    public abstract PhraseDao phraseDao();
    private static volatile PhraseRoomDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    static PhraseRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (PhraseRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), PhraseRoomDatabase.class, "phrase_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }

}
