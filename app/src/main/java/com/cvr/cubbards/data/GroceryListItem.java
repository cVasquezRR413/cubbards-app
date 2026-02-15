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
                        entity = Store.class,
                        parentColumns = "storeId",
                        childColumns = "storeId",
                        onDelete = ForeignKey.SET_NULL,
                        onUpdate = ForeignKey.NO_ACTION
                )
        },
        indices = {
                @Index(value = {"nameNormalized"}),
                @Index(value = {"storeId"})
        }
)
public class GroceryListItem {

    @PrimaryKey(autoGenerate = true)
    public long id;

    // 🆕 Grocery owns its own name
    public String name;
    public String nameNormalized;

    // Optional store link
    public Long storeId;

    public long addedAt;

    public double quantity;

    public String unit;

    // Primary constructor
    public GroceryListItem(String name,
                           String nameNormalized,
                           Long storeId,
                           long addedAt,
                           double quantity,
                           String unit) {
        this.name = name;
        this.nameNormalized = nameNormalized;
        this.storeId = storeId;
        this.addedAt = addedAt;
        this.quantity = quantity;
        this.unit = unit;
    }

    // Convenience constructor (no store)
    @Ignore
    public GroceryListItem(String name,
                           String nameNormalized,
                           long addedAt,
                           double quantity,
                           String unit) {
        this(name, nameNormalized, null, addedAt, quantity, unit);
    }
}