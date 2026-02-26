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

    // delete by grocery row id
    @Query("DELETE FROM grocery_list_items WHERE id = :groceryItemId")
    int deleteById(long groceryItemId);

    // toggle completion
    @Query("UPDATE grocery_list_items SET isCompleted = :completed WHERE id = :groceryItemId")
    int setCompleted(long groceryItemId, boolean completed);

    // ✅ UPDATED: include buyQuantity
    @Query(
            "UPDATE grocery_list_items SET " +
                    "name = :name, " +
                    "nameNormalized = :nameNormalized, " +
                    "quantity = :quantity, " +
                    "unit = :unit, " +
                    "buyQuantity = :buyQuantity, " +   // ✅ added
                    "priceCents = :priceCents, " +
                    "storeId = :storeId " +
                    "WHERE id = :groceryItemId"
    )
    int updateItem(long groceryItemId,
                   String name,
                   String nameNormalized,
                   double quantity,
                   String unit,
                   int buyQuantity,              // ✅ added
                   Integer priceCents,
                   Long storeId);

    @Query(
            "SELECT " +
                    "gli.id AS groceryItemId, " +
                    "gli.name AS name, " +
                    "gli.nameNormalized AS nameNormalized, " +
                    "gli.isCompleted AS isCompleted, " +
                    "gli.addedAt AS addedAt, " +
                    "gli.quantity AS quantity, " +
                    "gli.unit AS unit, " +
                    "gli.buyQuantity AS buyQuantity, " +   // ✅ added
                    "gli.priceCents AS priceCents, " +
                    "gli.storeId AS storeId, " +
                    "s.name AS storeName, " +
                    "s.location AS storeLocation " +
                    "FROM grocery_list_items gli " +
                    "LEFT JOIN stores s ON s.storeId = gli.storeId " +
                    "ORDER BY " +
                    "CASE WHEN s.name IS NULL THEN 1 ELSE 0 END, " +
                    "s.name ASC, " +
                    "s.location ASC, " +
                    "gli.addedAt DESC"
    )
    List<GroceryRow> getAll();
}
