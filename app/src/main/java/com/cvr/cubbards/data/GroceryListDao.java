// GroceryListDao.java (replace getAll() with this version)
package com.cvr.cubbards.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface GroceryListDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(GroceryListItem item);

    @Query("DELETE FROM grocery_list_items WHERE ingredientId = :ingredientId")
    int removeByIngredientId(long ingredientId);

    @Query(
            "SELECT " +
                    "gli.id AS groceryItemId, " +
                    "gli.ingredientId AS ingredientId, " +
                    "i.name AS ingredientName, " +
                    "gli.addedAt AS addedAt, " +
                    "gli.quantity AS quantity, " +
                    "gli.unit AS unit " +
                    "FROM grocery_list_items gli " +
                    "JOIN ingredients i ON i.ingredientId = gli.ingredientId " +
                    "ORDER BY gli.addedAt DESC"
    )
    List<GroceryRow> getAll();
}
