package com.cvr.cubbards.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "grocery_list_items",
        foreignKeys = {
                @ForeignKey(
                        entity = Ingredient.class,
                        parentColumns = "ingredientId",
                        childColumns = "ingredientId",
                        onDelete = ForeignKey.CASCADE,
                        onUpdate = ForeignKey.NO_ACTION
                ),
                @ForeignKey(
                        entity = Store.class,
                        parentColumns = "storeId",
                        childColumns = "storeId",
                        onDelete = ForeignKey.SET_NULL,
                        onUpdate = ForeignKey.NO_ACTION
                )
        },
        indices = {
                @Index(value = {"ingredientId"}, unique = true),
                @Index(value = {"storeId"})
        }
)
public class GroceryListItem {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long ingredientId;

    // NEW: optional store link
    public Long storeId;

    public long addedAt;

    public double quantity;

    public String unit;

    // Room constructor (use this one going forward).
    public GroceryListItem(long ingredientId, Long storeId, long addedAt, double quantity, String unit) {
        this.ingredientId = ingredientId;
        this.storeId = storeId;
        this.addedAt = addedAt;
        this.quantity = quantity;
        this.unit = unit;
    }

    // Backward-compatible convenience constructor for existing call sites
    @Ignore
    public GroceryListItem(long ingredientId, long addedAt, double quantity, String unit) {
        this(ingredientId, null, addedAt, quantity, unit);
    }

    // Older convenience constructor
    @Ignore
    public GroceryListItem(long ingredientId, long addedAt) {
        this(ingredientId, null, addedAt, 0.0, null);
    }
}
