package com.cvr.cubbards.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "price_observations",
        foreignKeys = {
                @ForeignKey(
                        entity = Ingredient.class,
                        parentColumns = "ingredientId",
                        childColumns = "ingredientId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = Store.class,
                        parentColumns = "storeId",
                        childColumns = "storeId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index("ingredientId"),
                @Index("storeId"),
                @Index(value = {"ingredientId", "storeId", "observedAt"})
        }
)
public class PriceObservation {

    @PrimaryKey(autoGenerate = true)
    private long observationId;

    private long ingredientId;

    private long storeId;

    private int priceCents;

    private long observedAt;

    public PriceObservation(long ingredientId, long storeId, int priceCents, long observedAt) {
        this.ingredientId = ingredientId;
        this.storeId = storeId;
        this.priceCents = priceCents;
        this.observedAt = observedAt;
    }

    public long getObservationId() {
        return observationId;
    }

    public void setObservationId(long observationId) {
        this.observationId = observationId;
    }

    public long getIngredientId() {
        return ingredientId;
    }

    public long getStoreId() {
        return storeId;
    }

    public int getPriceCents() {
        return priceCents;
    }

    public long getObservedAt() {
        return observedAt;
    }
}
