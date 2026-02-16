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
                            )
                            .addMigrations(
                                    AppDatabase.MIGRATION_1_2,
                                    AppDatabase.MIGRATION_2_3,
                                    AppDatabase.MIGRATION_3_4,
                                    AppDatabase.MIGRATION_4_5,
                                    AppDatabase.MIGRATION_5_6,
                                    AppDatabase.MIGRATION_6_7,
                                    AppDatabase.MIGRATION_7_8,
                                    AppDatabase.MIGRATION_8_9,
                                    AppDatabase.MIGRATION_9_10
                            )
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}