package com.cvr.cubbards.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
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

    public GroceryListItem(long ingredientId, long addedAt) {
        this.ingredientId = ingredientId;
        this.addedAt = addedAt;
    }
}
