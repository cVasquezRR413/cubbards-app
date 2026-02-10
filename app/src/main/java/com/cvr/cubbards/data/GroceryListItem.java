package com.cvr.cubbards.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "grocery_list_items",
        foreignKeys = @ForeignKey(
                entity = Ingredient.class,
                parentColumns = "ingredientId",
                childColumns = "ingredientId",
                onDelete = ForeignKey.CASCADE,
                onUpdate = ForeignKey.NO_ACTION
        ),
        indices = {
                @Index(value = {"ingredientId"}, unique = true)
        }
)
public class GroceryListItem {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long ingredientId;

    public long addedAt;

    // NEW: quantity + unit saved with the grocery list item
    public double quantity;

    // Keep it simple for now; can evolve into an enum later
    public String unit;

    /**
     * Room constructor (use this one going forward).
     */
    public GroceryListItem(long ingredientId, long addedAt, double quantity, String unit) {
        this.ingredientId = ingredientId;
        this.addedAt = addedAt;
        this.quantity = quantity;
        this.unit = unit;
    }

    /**
     * Backward-compatible convenience constructor for existing call sites.
     * Defaults quantity to 0 and unit to null.
     */
    @Ignore
    public GroceryListItem(long ingredientId, long addedAt) {
        this(ingredientId, addedAt, 0.0, null);
    }
}
