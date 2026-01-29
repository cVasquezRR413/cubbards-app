package com.cvr.cubbards.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
        entities = {
                Ingredient.class,
                Store.class,
                PriceObservation.class,
                PantryItem.class
        },
        version = 2,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract IngredientDao ingredientDao();

    public abstract StoreDao storeDao();

    public abstract PriceObservationDao priceObservationDao();

    public abstract PantryItemDao pantryItemDao();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pantry_items` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`ingredientId` INTEGER NOT NULL, " +
                            "`quantity` REAL NOT NULL, " +
                            "`unit` TEXT, " +
                            "FOREIGN KEY(`ingredientId`) REFERENCES `ingredients`(`ingredientId`) " +
                            "ON UPDATE NO ACTION ON DELETE CASCADE" +
                            ")"
            );

            db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_pantry_items_ingredientId` " +
                            "ON `pantry_items` (`ingredientId`)"
            );
        }
    };
}
