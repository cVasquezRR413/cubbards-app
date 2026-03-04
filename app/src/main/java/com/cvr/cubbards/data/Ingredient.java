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

    // manual "frequently replaced" pin
    private boolean isFrequent;

    public Ingredient(String name, String nameNormalized, Long createdAt) {
        this.name = name;
        this.nameNormalized = nameNormalized;
        this.createdAt = createdAt;
        this.isFrequent = false;
    }

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

    public boolean isFrequent() {
        return isFrequent;
    }

    public void setFrequent(boolean frequent) {
        isFrequent = frequent;
    }
}
