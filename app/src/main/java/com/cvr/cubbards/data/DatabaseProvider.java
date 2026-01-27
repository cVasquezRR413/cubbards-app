package com.cvr.cubbards.data;

import android.content.Context;

import androidx.room.Room;

public class DatabaseProvider {

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (DatabaseProvider.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "cubbards.db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
