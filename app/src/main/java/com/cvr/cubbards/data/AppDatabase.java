package com.cvr.cubbards.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                Ingredient.class,
                Store.class,
                PriceObservation.class
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract IngredientDao ingredientDao();

    public abstract StoreDao storeDao();

    public abstract PriceObservationDao priceObservationDao();
}
