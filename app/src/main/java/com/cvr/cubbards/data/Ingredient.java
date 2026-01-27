package com.cvr.cubbards.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "ingredients",
        indices = {
                @Index(value = {"nameNormalized"}, unique = true)
        }
)
public class Ingredient {

    @PrimaryKey(autoGenerate = true)
    private long ingredientId;

    private String name;

    private String nameNormalized;

    private Long createdAt;

    public Ingredient(String name, String nameNormalized, Long createdAt) {
        this.name = name;
        this.nameNormalized = nameNormalized;
        this.createdAt = createdAt;
    }

    // --- Getters and setters ---

    public long getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(long ingredientId) {
        this.ingredientId = ingredientId;
    }

    public String getName() {
        return name;
    }

    public String getNameNormalized() {
        return nameNormalized;
    }

    public Long getCreatedAt() {
        return createdAt;
    }
}
