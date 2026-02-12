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
        version = 8,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract IngredientDao ingredientDao();
    public abstract StoreDao storeDao();
    public abstract PriceObservationDao priceObservationDao();
    public abstract PantryItemDao pantryItemDao();
    public abstract GroceryListDao groceryListDao();

    // 1 → 2 : pantry_items
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

    // 2 → 3 : grocery_list_items
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

    // 3 → 4 : pantry_items.expiresAt
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `pantry_items` ADD COLUMN `expiresAt` INTEGER");
        }
    };

    // 4 → 5 : ingredients.isFrequent
    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL(
                    "ALTER TABLE `ingredients` " +
                            "ADD COLUMN `isFrequent` INTEGER NOT NULL DEFAULT 0"
            );
        }
    };

    // 5 → 6 : grocery_list_items.quantity + unit
    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL(
                    "ALTER TABLE `grocery_list_items` " +
                            "ADD COLUMN `quantity` REAL NOT NULL DEFAULT 0"
            );
            db.execSQL(
                    "ALTER TABLE `grocery_list_items` " +
                            "ADD COLUMN `unit` TEXT"
            );
        }
    };

    // 6 → 7 : stores.location
    public static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `stores` ADD COLUMN `location` TEXT");
        }
    };

    // 7 → 8 : grocery_list_items.storeId + FK to stores (table rebuild required)
    public static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {

            // Create new table with full schema + foreign keys
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `grocery_list_items_new` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`ingredientId` INTEGER NOT NULL, " +
                            "`storeId` INTEGER, " +
                            "`addedAt` INTEGER NOT NULL, " +
                            "`quantity` REAL NOT NULL DEFAULT 0, " +
                            "`unit` TEXT, " +
                            "FOREIGN KEY(`ingredientId`) REFERENCES `ingredients`(`ingredientId`) " +
                            "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                            "FOREIGN KEY(`storeId`) REFERENCES `stores`(`storeId`) " +
                            "ON UPDATE NO ACTION ON DELETE SET NULL" +
                            ")"
            );

            // Copy existing data (storeId did NOT exist before v8, so use NULL)
            db.execSQL(
                    "INSERT INTO `grocery_list_items_new` " +
                            "(`id`, `ingredientId`, `storeId`, `addedAt`, `quantity`, `unit`) " +
                            "SELECT `id`, `ingredientId`, NULL, `addedAt`, `quantity`, `unit` " +
                            "FROM `grocery_list_items`"
            );

            // Replace old table
            db.execSQL("DROP TABLE `grocery_list_items`");
            db.execSQL("ALTER TABLE `grocery_list_items_new` RENAME TO `grocery_list_items`");

            // Recreate indexes
            db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_grocery_list_items_ingredientId` " +
                            "ON `grocery_list_items` (`ingredientId`)"
            );
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_grocery_list_items_storeId` " +
                            "ON `grocery_list_items` (`storeId`)"
            );
        }
    };
}
