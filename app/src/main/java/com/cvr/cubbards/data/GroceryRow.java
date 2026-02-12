package com.cvr.cubbards.data;

public class GroceryRow {
    public long groceryItemId;
    public long ingredientId;
    public String ingredientName;
    public long addedAt;

    public double quantity;
    public String unit;

    // NEW: optional store info for grouping
    public Long storeId;
    public String storeName;
    public String storeLocation;
}
