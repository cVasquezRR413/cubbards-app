package com.cvr.cubbards.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "pantry_items",
        foreignKeys = @ForeignKey(
                entity = Ingredient.class,
                parentColumns = {"ingredientId"},
                childColumns = {"ingredientId"},
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index(value = {"ingredientId"}, unique = true)
        }
)
public class PantryItem {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long ingredientId;

    public double quantity;

    public String unit;

    public PantryItem(long ingredientId, double quantity, String unit) {
        this.ingredientId = ingredientId;
        this.quantity = quantity;
        this.unit = unit;
    }
}
