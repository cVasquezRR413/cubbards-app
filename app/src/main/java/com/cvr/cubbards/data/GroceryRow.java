package com.cvr.cubbards.data;

public class GroceryRow {

    public long groceryItemId;

    // 🆕 Grocery owns its own name now
    public String name;
    public String nameNormalized;

    public long addedAt;

    public double quantity;
    public String unit;

    // Optional store info for grouping
    public Long storeId;
    public String storeName;
    public String storeLocation;
}