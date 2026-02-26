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
        version = 13,
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

            db.execSQL(
                    "INSERT INTO `grocery_list_items_new` " +
                            "(`id`, `ingredientId`, `storeId`, `addedAt`, `quantity`, `unit`) " +
                            "SELECT `id`, `ingredientId`, NULL, `addedAt`, `quantity`, `unit` " +
                            "FROM `grocery_list_items`"
            );

            db.execSQL("DROP TABLE `grocery_list_items`");
            db.execSQL("ALTER TABLE `grocery_list_items_new` RENAME TO `grocery_list_items`");

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

    // 8 → 9 : DECOUPLE grocery list from ingredients (table rebuild required)
    public static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {

            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `grocery_list_items_new` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`name` TEXT, " +
                            "`nameNormalized` TEXT, " +
                            "`storeId` INTEGER, " +
                            "`addedAt` INTEGER NOT NULL, " +
                            "`quantity` REAL NOT NULL DEFAULT 0, " +
                            "`unit` TEXT, " +
                            "FOREIGN KEY(`storeId`) REFERENCES `stores`(`storeId`) " +
                            "ON UPDATE NO ACTION ON DELETE SET NULL" +
                            ")"
            );

            db.execSQL(
                    "INSERT INTO `grocery_list_items_new` " +
                            "(`id`, `name`, `nameNormalized`, `storeId`, `addedAt`, `quantity`, `unit`) " +
                            "SELECT " +
                            "gli.`id`, " +
                            "COALESCE(i.`name`, '(deleted)') AS `name`, " +
                            "COALESCE(i.`nameNormalized`, LOWER(COALESCE(i.`name`, '(deleted)'))) AS `nameNormalized`, " +
                            "gli.`storeId`, " +
                            "gli.`addedAt`, " +
                            "gli.`quantity`, " +
                            "gli.`unit` " +
                            "FROM `grocery_list_items` gli " +
                            "LEFT JOIN `ingredients` i ON i.`ingredientId` = gli.`ingredientId`"
            );

            db.execSQL("DROP TABLE `grocery_list_items`");
            db.execSQL("ALTER TABLE `grocery_list_items_new` RENAME TO `grocery_list_items`");

            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_grocery_list_items_nameNormalized` " +
                            "ON `grocery_list_items` (`nameNormalized`)"
            );
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_grocery_list_items_storeId` " +
                            "ON `grocery_list_items` (`storeId`)"
            );
        }
    };

    // 9 → 10 : grocery_list_items.priceCents (optional)
    public static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `grocery_list_items` ADD COLUMN `priceCents` INTEGER");
        }
    };

    // 10 → 11 : grocery_list_items.isCompleted
    public static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL(
                    "ALTER TABLE `grocery_list_items` " +
                            "ADD COLUMN `isCompleted` INTEGER NOT NULL DEFAULT 0"
            );
        }
    };

    // 11 → 12 : enforce name length <= 60 via CHECK constraints (table rebuild required)
    public static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {

            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `grocery_list_items_new` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`name` TEXT, " +
                            "`nameNormalized` TEXT, " +
                            "`storeId` INTEGER, " +
                            "`addedAt` INTEGER NOT NULL, " +
                            "`quantity` REAL NOT NULL DEFAULT 0, " +
                            "`unit` TEXT, " +
                            "`priceCents` INTEGER, " +
                            "`isCompleted` INTEGER NOT NULL DEFAULT 0, " +
                            "FOREIGN KEY(`storeId`) REFERENCES `stores`(`storeId`) " +
                            "ON UPDATE NO ACTION ON DELETE SET NULL, " +
                            "CHECK(`name` IS NULL OR length(`name`) <= 60), " +
                            "CHECK(`nameNormalized` IS NULL OR length(`nameNormalized`) <= 60)" +
                            ")"
            );

            db.execSQL(
                    "INSERT INTO `grocery_list_items_new` " +
                            "(`id`, `name`, `nameNormalized`, `storeId`, `addedAt`, `quantity`, `unit`, `priceCents`, `isCompleted`) " +
                            "SELECT " +
                            "`id`, " +
                            "CASE WHEN `name` IS NULL THEN NULL ELSE substr(`name`, 1, 60) END, " +
                            "CASE WHEN `nameNormalized` IS NULL THEN NULL ELSE substr(`nameNormalized`, 1, 60) END, " +
                            "`storeId`, " +
                            "`addedAt`, " +
                            "`quantity`, " +
                            "`unit`, " +
                            "`priceCents`, " +
                            "`isCompleted` " +
                            "FROM `grocery_list_items`"
            );

            db.execSQL("DROP TABLE `grocery_list_items`");
            db.execSQL("ALTER TABLE `grocery_list_items_new` RENAME TO `grocery_list_items`");

            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_grocery_list_items_nameNormalized` " +
                            "ON `grocery_list_items` (`nameNormalized`)"
            );
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_grocery_list_items_storeId` " +
                            "ON `grocery_list_items` (`storeId`)"
            );
        }
    };

    // ✅ 12 → 13 : grocery_list_items.buyQuantity
    public static final Migration MIGRATION_12_13 = new Migration(12, 13) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL(
                    "ALTER TABLE `grocery_list_items` " +
                            "ADD COLUMN `buyQuantity` INTEGER NOT NULL DEFAULT 1"
            );
        }
    };
}
