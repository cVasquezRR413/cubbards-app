package com.cvr.cubbards.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PantryItemDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(PantryItem item);

    @Update
    int update(PantryItem item);

    @Delete
    int delete(PantryItem item);

    @Query("SELECT * FROM pantry_items ORDER BY ingredientId ASC")
    List<PantryItem> getAll();

    @Query(
            "SELECT pi.id AS pantryItemId, " +
                    "pi.ingredientId AS ingredientId, " +
                    "i.name AS ingredientName, " +
                    "pi.quantity AS quantity, " +
                    "pi.unit AS unit " +
                    "FROM pantry_items pi " +
                    "JOIN ingredients i ON i.ingredientId = pi.ingredientId " +
                    "ORDER BY i.name ASC"
    )
    List<PantryRow> getPantryRows();
}
