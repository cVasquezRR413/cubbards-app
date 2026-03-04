package com.cvr.cubbards.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface IngredientDao {

    @Insert
    long insert(Ingredient ingredient);

    @Update
    int update(Ingredient ingredient);

    @Delete
    int delete(Ingredient ingredient);

    @Query("SELECT * FROM ingredients ORDER BY name ASC")
    List<Ingredient> getAll();

    @Query("SELECT * FROM ingredients WHERE ingredientId = :id LIMIT 1")
    Ingredient getById(long id);

    @Query("UPDATE ingredients SET isFrequent = :isFrequent WHERE ingredientId = :ingredientId")
    int setFrequent(long ingredientId, boolean isFrequent);

    @Query("SELECT * FROM ingredients WHERE isFrequent = 1 ORDER BY name ASC")
    List<Ingredient> getFrequentIngredients();

    @Query("SELECT * FROM ingredients WHERE nameNormalized = :nameNormalized LIMIT 1")
    Ingredient getByNameNormalized(String nameNormalized);

}
