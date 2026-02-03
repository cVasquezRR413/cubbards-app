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
                PantryItem.class,
                GroceryListItem.class
        },
        version = 5,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract IngredientDao ingredientDao();
    public abstract StoreDao storeDao();
    public abstract PriceObservationDao priceObservationDao();
    public abstract PantryItemDao pantryItemDao();
    public abstract GroceryListDao groceryListDao();

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

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `grocery_list_items` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`ingredientId` INTEGER NOT NULL, " +
                            "`addedAt` INTEGER NOT NULL, " +
                            "FOREIGN KEY(`ingredientId`) REFERENCES `ingredients`(`ingredientId`) " +
                            "ON UPDATE NO ACTION ON DELETE CASCADE" +
                            ")"
            );

            db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_grocery_list_items_ingredientId` " +
                            "ON `grocery_list_items` (`ingredientId`)"
            );
        }
    };

    // add expiresAt column to pantry_items
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `pantry_items` ADD COLUMN `expiresAt` INTEGER");
        }
    };

    // NEW: add isFrequent column to ingredients
    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL(
                    "ALTER TABLE `ingredients` " +
                            "ADD COLUMN `isFrequent` INTEGER NOT NULL DEFAULT 0"
            );
        }
    };
}
