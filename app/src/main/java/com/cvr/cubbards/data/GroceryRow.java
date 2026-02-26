package com.cvr.cubbards.data;

public class GroceryRow {

    public long groceryItemId;

    // Grocery owns its own name now
    public String name;
    public String nameNormalized;

    public long addedAt;

    // Unit-size / details line
    public double quantity;
    public String unit;

    // ✅ NEW: how many the user plans to buy (for "(2) Almond milk")
    public int buyQuantity;

    // Optional store info for grouping
    public Long storeId;
    public String storeName;
    public String storeLocation;

    public Integer priceCents;

    public boolean isCompleted;
}
