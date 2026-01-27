package com.cvr.cubbards.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PriceObservationDao {

    @Insert
    long insert(PriceObservation observation);

    @Update
    int update(PriceObservation observation);

    @Delete
    int delete(PriceObservation observation);

    @Query("SELECT * FROM price_observations ORDER BY observedAt DESC")
    List<PriceObservation> getAll();

    @Query(
            "SELECT * FROM price_observations " +
                    "WHERE ingredientId = :ingredientId AND storeId = :storeId " +
                    "ORDER BY observedAt DESC"
    )
    List<PriceObservation> getByIngredientAndStore(long ingredientId, long storeId);
}
