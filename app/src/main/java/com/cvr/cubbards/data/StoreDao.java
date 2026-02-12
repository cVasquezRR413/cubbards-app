package com.cvr.cubbards.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface StoreDao {

    @Insert
    long insert(Store store);

    @Update
    int update(Store store);

    @Delete
    int delete(Store store);

    @Query("SELECT * FROM stores ORDER BY name ASC")
    List<Store> getAll();

    @Query("SELECT * FROM stores WHERE storeId = :id LIMIT 1")
    Store getById(long id);

    // Existing behavior (still useful)
    @Query("SELECT * FROM stores WHERE name = :name AND location IS NULL LIMIT 1")
    Store getByName(String name);

    // NEW: correct lookup when location exists
    @Query(
            "SELECT * FROM stores " +
                    "WHERE name = :name AND " +
                    "((:location IS NULL AND location IS NULL) OR location = :location) " +
                    "LIMIT 1"
    )
    Store getByNameAndLocation(String name, String location);
}
